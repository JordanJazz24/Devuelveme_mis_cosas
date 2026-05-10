package com.example.devuelveme_mis_cosas.presentation.new_loan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewLoanUiState(
    val nombreObjeto: String = "",
    val contactoNombre: String = "",
    val contactoTelefono: String = "",
    val fechaDevolucion: Long = System.currentTimeMillis() + 86400000 * 7, // +1 semana
    val photoUri: Uri? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class NewLoanViewModel @Inject constructor(
    private val repository: LoanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewLoanUiState())
    val uiState: StateFlow<NewLoanUiState> = _uiState.asStateFlow()

    fun onNombreObjetoChange(newValue: String) {
        _uiState.update { it.copy(nombreObjeto = newValue) }
    }

    fun onContactoNombreChange(newValue: String) {
        _uiState.update { it.copy(contactoNombre = newValue) }
    }

    fun onContactoTelefonoChange(newValue: String) {
        _uiState.update { it.copy(contactoTelefono = newValue) }
    }

    fun onFechaDevolucionChange(newValue: Long) {
        _uiState.update { it.copy(fechaDevolucion = newValue) }
    }

    fun onPhotoCaptured(uri: Uri?) {
        _uiState.update { it.copy(photoUri = uri) }
    }

    fun saveLoan() {
        val currentState = _uiState.value
        if (currentState.nombreObjeto.isBlank() || currentState.contactoNombre.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val newLoan = LoanEntity(
                nombreObjeto = currentState.nombreObjeto,
                contactoNombre = currentState.contactoNombre,
                contactoTelefono = currentState.contactoTelefono,
                fechaPrestamo = System.currentTimeMillis(),
                fechaDevolucion = currentState.fechaDevolucion,
                photoLoanUri = currentState.photoUri?.toString(),
                photoReturnUri = null,
                estado = LoanStatus.ACTIVO
            )
            repository.insertLoan(newLoan)
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }
}
