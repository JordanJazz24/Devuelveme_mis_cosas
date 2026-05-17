package com.example.devuelveme_mis_cosas.presentation.loan_detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import com.example.devuelveme_mis_cosas.domain.util.ReminderMessageBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LoanDetailViewModel @Inject constructor(
    private val repository: LoanRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val loanIdString: String = checkNotNull(savedStateHandle["loanId"])
    private val loanId: UUID = UUID.fromString(loanIdString)

    val loan: StateFlow<LoanEntity?> = repository.getLoanById(loanId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun sendReminder(context: Context) {
        val currentLoan = loan.value ?: return
        val message = ReminderMessageBuilder.buildMessage(currentLoan)
        val phoneNumber = currentLoan.contactoTelefono.filter { it.isDigit() }

        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "El contacto no tiene un teléfono válido", Toast.LENGTH_SHORT).show()
            return
        }

        val whatsappUrl = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(whatsappUrl)
        }

        try {
            context.startActivity(intent)
            updateReminderStats(currentLoan)
        } catch (e: Exception) {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
            }
            try {
                context.startActivity(smsIntent)
                updateReminderStats(currentLoan)
            } catch (ex: Exception) {
                Toast.makeText(context, "No se pudo abrir WhatsApp ni SMS", Toast.LENGTH_SHORT).show()
            }
        }
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
                    fechaDevolucionReal = Date() // Fecha actual como fecha real de entrega
                )
            )
        }
    }

    fun deleteLoan() {
        val currentLoan = loan.value ?: return
        viewModelScope.launch {
            repository.deleteLoan(currentLoan)
        }
    }
}
