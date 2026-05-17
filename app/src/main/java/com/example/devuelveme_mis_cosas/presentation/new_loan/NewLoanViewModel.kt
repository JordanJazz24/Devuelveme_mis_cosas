package com.example.devuelveme_mis_cosas.presentation.new_loan

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.devuelveme_mis_cosas.data.local.LoanCategory
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import com.example.devuelveme_mis_cosas.domain.model.Contact
import com.example.devuelveme_mis_cosas.domain.repository.ContactsRepository
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

data class NewLoanUiState(
    val nombreObjeto: String = "",
    val contactoNombre: String = "",
    val contactoTelefono: String = "",
    val contactoPhotoUri: String? = null,
    val categoria: LoanCategory = LoanCategory.OTROS,
    val fechaPrestamo: Date = Date(),
    val fechaDevolucion: Date = Date(),
    val photoUri: Uri? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val contacts: List<Contact> = emptyList(),
    val showContactPicker: Boolean = false
)

@HiltViewModel
class NewLoanViewModel @Inject constructor(
    private val repository: LoanRepository,
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewLoanUiState())
    val uiState: StateFlow<NewLoanUiState> = _uiState.asStateFlow()

    init {
        val now = System.currentTimeMillis()
        _uiState.update { 
            it.copy(
                fechaPrestamo = normalizeDateToLocalMidday(now),
                fechaDevolucion = normalizeDateToLocalMidday(now + 86400000 * 7) // +1 week
            )
        }
    }

    private fun loadContacts() {
        viewModelScope.launch {
            try {
                contactsRepository.getContacts().collect { contacts ->
                    _uiState.update { it.copy(contacts = contacts) }
                }
            } catch (e: Exception) {
                Log.e("NewLoanVM", "Error loading contacts", e)
            }
        }
    }

    fun onNombreObjetoChange(newValue: String) {
        _uiState.update { it.copy(nombreObjeto = newValue, errorMessage = null) }
    }

    fun onContactoNombreChange(newValue: String) {
        _uiState.update { it.copy(contactoNombre = newValue, errorMessage = null) }
    }

    fun onContactoTelefonoChange(newValue: String) {
        _uiState.update { it.copy(contactoTelefono = newValue) }
    }

    fun onCategoriaChange(newValue: LoanCategory) {
        _uiState.update { it.copy(categoria = newValue) }
    }

    fun onFechaPrestamoChange(newValue: Long) {
        _uiState.update { it.copy(fechaPrestamo = normalizeDateToLocalMidday(newValue), errorMessage = null) }
    }

    fun onFechaDevolucionChange(newValue: Long) {
        _uiState.update { it.copy(fechaDevolucion = normalizeDateToLocalMidday(newValue), errorMessage = null) }
    }

    fun getUtcMillis(date: Date): Long {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.set(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
        utcCalendar.set(Calendar.MILLISECOND, 0)
        return utcCalendar.timeInMillis
    }

    private fun normalizeDateToLocalMidday(millis: Long): Date {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.timeInMillis = millis
        
        val result = Calendar.getInstance()
        result.set(
            utcCalendar.get(Calendar.YEAR),
            utcCalendar.get(Calendar.MONTH),
            utcCalendar.get(Calendar.DAY_OF_MONTH),
            12, 0, 0
        )
        result.set(Calendar.MILLISECOND, 0)
        return result.time
    }

    fun onPhotoSelected(uri: Uri?) {
        _uiState.update { it.copy(photoUri = uri) }
    }

    fun toggleContactPicker(show: Boolean) {
        _uiState.update { it.copy(showContactPicker = show) }
        if (show) loadContacts()
    }

    fun onContactSelected(contact: Contact) {
        _uiState.update {
            it.copy(
                contactoNombre = contact.name,
                contactoTelefono = contact.phoneNumber ?: "",
                contactoPhotoUri = contact.photoUri,
                showContactPicker = false
            )
        }
    }

    fun saveLoan() {
        val currentState = _uiState.value
        
        if (currentState.nombreObjeto.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor, indica qué has prestado") }
            return
        }
        if (currentState.contactoNombre.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor, selecciona un contacto") }
            return
        }
        
        if (currentState.fechaPrestamo.after(currentState.fechaDevolucion)) {
            _uiState.update { it.copy(errorMessage = "La fecha de préstamo no puede ser posterior a la de devolución") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }
                val newLoan = LoanEntity(
                    id = UUID.randomUUID(),
                    nombreObjeto = currentState.nombreObjeto,
                    contactoNombre = currentState.contactoNombre,
                    contactoTelefono = currentState.contactoTelefono,
                    contactoPhotoUri = currentState.contactoPhotoUri,
                    fechaPrestamo = currentState.fechaPrestamo,
                    fechaDevolucion = currentState.fechaDevolucion,
                    photoLoanUri = currentState.photoUri?.toString(),
                    photoReturnUri = null,
                    estado = LoanStatus.ACTIVO,
                    categoria = currentState.categoria
                )
                repository.insertLoan(newLoan)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                Log.e("NewLoanVM", "Error al guardar préstamo", e)
                _uiState.update { it.copy(isSaving = false, errorMessage = "Error al guardar: ${e.message}") }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
