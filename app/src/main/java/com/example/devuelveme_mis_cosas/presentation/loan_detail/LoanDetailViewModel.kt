package com.example.devuelveme_mis_cosas.presentation.loan_detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import com.example.devuelveme_mis_cosas.domain.util.ReminderMessageBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class LoanDetailUiState(
    val reminderMessage: String? = null,
    val reminderError: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class LoanDetailViewModel @Inject constructor(
    private val repository: LoanRepository,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val loanIdString: String = checkNotNull(savedStateHandle["loanId"])
    private val loanId: UUID = UUID.fromString(loanIdString)

    private val _uiState = MutableStateFlow(LoanDetailUiState())
    val uiState: StateFlow<LoanDetailUiState> = _uiState

    val loan: StateFlow<LoanEntity?> = repository.getLoanById(loanId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun sendReminder() {
        val currentLoan = loan.value ?: return
        val phoneNumber = currentLoan.contactoTelefono.filter { it.isDigit() }

        if (phoneNumber.isBlank()) {
            _uiState.update {
                it.copy(
                    reminderMessage = "El contacto no tiene número de teléfono registrado.",
                    reminderError = true
                )
            }
            return
        }

        val message = ReminderMessageBuilder.buildMessage(currentLoan)
        val whatsappUrl = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(whatsappUrl)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
            _uiState.update {
                it.copy(
                    reminderMessage = "Recordatorio enviado por WhatsApp ✓",
                    reminderError = false
                )
            }
            updateReminderStats(currentLoan)
        } catch (e: ActivityNotFoundException) {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(smsIntent)
                _uiState.update {
                    it.copy(
                        reminderMessage = "WhatsApp no disponible, abriendo SMS...",
                        reminderError = false
                    )
                }
                updateReminderStats(currentLoan)
            } catch (ex: Exception) {
                _uiState.update {
                    it.copy(
                        reminderMessage = "No se pudo abrir WhatsApp ni SMS.",
                        reminderError = true
                    )
                }
            }
        }
    }

    fun clearReminderMessage() {
        _uiState.update { it.copy(reminderMessage = null) }
    }

    private fun updateReminderStats(loan: LoanEntity) {
        viewModelScope.launch {
            repository.updateLoan(
                loan.copy(
                    reminderCount = loan.reminderCount + 1,
                    lastReminderTimestamp = Date()
                )
            )
        }
    }

    fun markAsReturned(photoReturnUri: String? = null) {
        val currentLoan = loan.value ?: return
        viewModelScope.launch {
            repository.updateLoan(
                currentLoan.copy(
                    estado = LoanStatus.DEVUELTO,
                    photoReturnUri = photoReturnUri,
                    fechaDevolucionReal = Date()
                )
            )
            // Cancelar recordatorios futuros
            WorkManager.getInstance(context).cancelAllWorkByTag(loanId.toString())
            _uiState.update { it.copy(saveSuccess = true) }
        }
    }

    fun deleteLoan() {
        val currentLoan = loan.value ?: return
        viewModelScope.launch {
            repository.deleteLoan(currentLoan)
            // Cancelar recordatorios programados
            WorkManager.getInstance(context).cancelAllWorkByTag(loanId.toString())
        }
    }
}
