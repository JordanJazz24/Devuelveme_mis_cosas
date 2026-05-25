package com.example.devuelveme_mis_cosas.presentation.loan_detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.devuelveme_mis_cosas.data.local.ContactReputation
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import com.example.devuelveme_mis_cosas.domain.repository.ContactReputationRepository
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import com.example.devuelveme_mis_cosas.domain.util.ReminderMessageBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class LoanDetailUiState(
    val reminderMessage: String? = null,
    val reminderError: Boolean = false
)

@HiltViewModel
class LoanDetailViewModel @Inject constructor(
    private val repository: LoanRepository,
    private val contactReputationRepository: ContactReputationRepository,
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
            _uiState.update { it.copy(reminderMessage = "El contacto no tiene número de teléfono registrado.", reminderError = true) }
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
            _uiState.update { it.copy(reminderMessage = "Recordatorio enviado por WhatsApp ✓", reminderError = false) }
            updateReminderStats(currentLoan)
        } catch (e: ActivityNotFoundException) {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(smsIntent)
                _uiState.update { it.copy(reminderMessage = "WhatsApp no disponible, abriendo SMS...", reminderError = false) }
                updateReminderStats(currentLoan)
            } catch (ex: Exception) {
                _uiState.update { it.copy(reminderMessage = "No se pudo abrir WhatsApp ni SMS.", reminderError = true) }
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

    fun markAsReturnedWithCondition(photoReturnUri: String? = null, condition: String) {
        val currentLoan = loan.value ?: return
        viewModelScope.launch {
            val updatedLoan = currentLoan.copy(
                estado = LoanStatus.DEVUELTO,
                photoReturnUri = photoReturnUri,
                fechaDevolucionReal = Date(),
                returnCondition = condition
            )
            repository.updateLoan(updatedLoan)
            recalculateContactReputation(updatedLoan)
            WorkManager.getInstance(context).cancelAllWorkByTag(loanId.toString())
        }
    }

    private suspend fun recalculateContactReputation(lastLoan: LoanEntity) {
        val allLoans = repository.getLoansForContact(lastLoan.contactoTelefono).first()

        var returnedOnTime = 0
        var returnedLate = 0
        var returnedDamaged = 0
        var neverReturned = 0
        var excellentReturns = 0

        allLoans.forEach { loanEntity ->
            if (loanEntity.estado == LoanStatus.DEVUELTO) {
                val isLate = loanEntity.fechaDevolucionReal?.after(loanEntity.fechaDevolucion) ?: false
                if (isLate) returnedLate++ else returnedOnTime++

                when (loanEntity.returnCondition) {
                    "EXCELENTE" -> excellentReturns++
                    "MALO" -> returnedDamaged++
                    "NUNCA_DEVUELTO" -> neverReturned++
                }
            }
        }

        // Lógica de score: base 5.0, resta 1.0 tarde, resta 2.0 MALO, resta 5.0 NUNCA, suma 0.5 EXCELENTE
        var score = 5.0f
        score -= (returnedLate * 1.0f)
        score -= (returnedDamaged * 2.0f)
        score -= (neverReturned * 5.0f)
        score += (excellentReturns * 0.5f)
        score = score.coerceIn(0.0f, 5.0f)

        val reputation = ContactReputation(
            contactPhone = lastLoan.contactoTelefono,
            contactName = lastLoan.contactoNombre,
            contactPhotoUri = lastLoan.contactoPhotoUri,
            totalLoans = allLoans.size,
            returnedOnTime = returnedOnTime,
            returnedLate = returnedLate,
            returnedDamaged = returnedDamaged,
            neverReturned = neverReturned,
            reputationScore = score
        )

        contactReputationRepository.upsert(reputation)
    }

    fun deleteLoan() {
        val currentLoan = loan.value ?: return
        viewModelScope.launch {
            repository.deleteLoan(currentLoan)
            WorkManager.getInstance(context).cancelAllWorkByTag(loanId.toString())
        }
    }
}
