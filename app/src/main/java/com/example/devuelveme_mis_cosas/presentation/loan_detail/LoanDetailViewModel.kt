package com.example.devuelveme_mis_cosas.presentation.loan_detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.devuelveme_mis_cosas.data.local.ContactReputation
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanPayment
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import com.example.devuelveme_mis_cosas.domain.repository.ContactReputationRepository
import com.example.devuelveme_mis_cosas.domain.repository.LoanPaymentRepository
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import com.example.devuelveme_mis_cosas.domain.util.DateUtils
import com.example.devuelveme_mis_cosas.domain.util.ReminderMessageBuilder
import com.example.devuelveme_mis_cosas.work.LoanReminderWorker
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class LoanDetailUiState(
    val reminderMessage: String? = null,
    val reminderError: Boolean = false,
    val saveSuccess: Boolean = false,
    val paymentSuccess: Boolean = false
)

@HiltViewModel
class LoanDetailViewModel @Inject constructor(
    private val repository: LoanRepository,
    private val contactReputationRepository: ContactReputationRepository,
    private val loanPaymentRepository: LoanPaymentRepository,
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

    val payments: StateFlow<List<LoanPayment>> = loanPaymentRepository.getPaymentsByLoanId(loanId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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

    fun markAsReturnedWithCondition(photoReturnUri: String? = null, condition: String, settleDebt: Boolean = false) {
        val currentLoan = loan.value ?: return
        viewModelScope.launch {
            val updatedLoan = currentLoan.copy(
                estado = LoanStatus.DEVUELTO,
                photoReturnUri = photoReturnUri,
                fechaDevolucionReal = Date(),
                returnCondition = condition,
                remainingAmount = if (settleDebt) 0.0 else currentLoan.remainingAmount
            )
            repository.updateLoan(updatedLoan)
            
            if (settleDebt && (currentLoan.remainingAmount ?: 0.0) > 0) {
                val payment = LoanPayment(
                    loanId = loanId,
                    amount = currentLoan.remainingAmount ?: 0.0,
                    note = "Saldado al cerrar préstamo"
                )
                loanPaymentRepository.insertPayment(payment)
            }

            recalculateContactReputation(updatedLoan)
            WorkManager.getInstance(context).cancelAllWorkByTag(loanId.toString())
            _uiState.update { it.copy(saveSuccess = true) }
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
                if (loanEntity.returnCondition == "NUNCA_DEVUELTO") {
                    neverReturned++
                } else {
                    // Solo si sí lo devolvió, calculamos si fue a tiempo o tarde
                    val isLate = loanEntity.fechaDevolucionReal?.after(loanEntity.fechaDevolucion) ?: false
                    if (isLate) returnedLate++ else returnedOnTime++

                    // Calculamos las condiciones extra
                    when (loanEntity.returnCondition) {
                        "EXCELENTE" -> excellentReturns++
                        "MALO" -> returnedDamaged++
                    }
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

    fun registerPayment(amountStr: String, note: String?) {
        val currentLoan = loan.value ?: return
        val amount = amountStr.toDoubleOrNull() ?: return
        if (amount <= 0) return

        viewModelScope.launch {
            val payment = LoanPayment(
                loanId = loanId,
                amount = amount,
                note = if (note.isNullOrBlank()) null else note
            )
            loanPaymentRepository.insertPayment(payment)

            val newRemaining = ((currentLoan.remainingAmount ?: 0.0) - amount).coerceAtLeast(0.0)
            
            // Actualizamos la entidad con el nuevo saldo
            val updatedLoan = currentLoan.copy(remainingAmount = newRemaining)
            repository.updateLoan(updatedLoan)

            if (newRemaining <= 0.01) {
                // Si ya no hay saldo, llamamos a la función que cierra el préstamo
                // Esta función también hace un update del préstamo, por lo que usamos el objeto actualizado
                markAsReturnedWithCondition(photoReturnUri = null, condition = "BUENO", settleDebt = true)
                _uiState.update { it.copy(reminderMessage = "¡Préstamo liquidado y cerrado! ✓", reminderError = false) }
            } else {
                _uiState.update { it.copy(reminderMessage = "Abono registrado correctamente", reminderError = false) }
            }
        }
    }

    fun updateReturnDate(newDateMillis: Long) {
        val currentLoan = loan.value ?: return
        val newDate = DateUtils.normalizeDateToLocalMidday(newDateMillis)
        
        if (newDate.before(currentLoan.fechaPrestamo)) {
            _uiState.update { it.copy(reminderMessage = "La fecha de devolución no puede ser anterior al préstamo", reminderError = true) }
            return
        }

        viewModelScope.launch {
            val updatedLoan = currentLoan.copy(fechaDevolucion = newDate)
            repository.updateLoan(updatedLoan)
            
            // Reprogramar recordatorios
            rescheduleWorkManager(updatedLoan)
            
            _uiState.update { it.copy(reminderMessage = "Fecha de devolución actualizada ✓", reminderError = false) }
        }
    }

    private fun rescheduleWorkManager(loan: LoanEntity) {
        val workManager = WorkManager.getInstance(context)
        val tag = loan.id.toString()
        
        // Cancelar existentes
        workManager.cancelAllWorkByTag(tag)
        
        // Programar nuevos
        val inputData = Data.Builder()
            .putString(LoanReminderWorker.KEY_LOAN_ID, loan.id.toString())
            .putString(LoanReminderWorker.KEY_CONTACTO_NOMBRE, loan.contactoNombre)
            .putString(LoanReminderWorker.KEY_NOMBRE_OBJETO, loan.nombreObjeto)
            .build()

        val delay7Days = (loan.fechaDevolucion.time - 7 * 86_400_000L) - System.currentTimeMillis()
        if (delay7Days > 0) {
            val request7Days = OneTimeWorkRequestBuilder<LoanReminderWorker>()
                .setInputData(inputData)
                .setInitialDelay(delay7Days, TimeUnit.MILLISECONDS)
                .addTag(tag)
                .build()
            workManager.enqueue(request7Days)
        }

        val delayDue = loan.fechaDevolucion.time - System.currentTimeMillis()
        if (delayDue > 0) {
            val requestDue = OneTimeWorkRequestBuilder<LoanReminderWorker>()
                .setInputData(inputData)
                .setInitialDelay(delayDue, TimeUnit.MILLISECONDS)
                .addTag(tag)
                .build()
            workManager.enqueue(requestDue)
        }
    }

    fun getUtcMillis(date: Date): Long = DateUtils.getUtcMillis(date)

    fun deleteLoan() {
        val currentLoan = loan.value ?: return
        viewModelScope.launch {
            loanPaymentRepository.deletePaymentsByLoanId(loanId)
            repository.deleteLoan(currentLoan)
            WorkManager.getInstance(context).cancelAllWorkByTag(loanId.toString())
            _uiState.update { it.copy(saveSuccess = true) }
        }
    }
}
