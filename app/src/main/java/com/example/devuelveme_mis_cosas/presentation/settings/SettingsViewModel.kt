package com.example.devuelveme_mis_cosas.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import com.example.devuelveme_mis_cosas.data.local.LoanCategory
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LoanRepository
) : ViewModel() {

    private val prefsName = "app_settings"
    private val keyDark = "dark_mode"

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val dark = prefs.getBoolean(keyDark, false)
        _uiState.update { it.copy(isDarkMode = dark) }
    }

    fun toggleDarkMode() {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val newValue = !_uiState.value.isDarkMode
        prefs.edit().putBoolean(keyDark, newValue).apply()
        _uiState.update { it.copy(isDarkMode = newValue) }
    }

    fun exportLoans(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val active = repository.getActiveLoans().first()
                val returned = repository.getReturnedLoans().first()

                val root = JSONObject()
                val activeArray = JSONArray()
                active.forEach { loan -> activeArray.put(loanToJson(loan)) }
                val returnedArray = JSONArray()
                returned.forEach { loan -> returnedArray.put(loanToJson(loan)) }
                
                root.put("active", activeArray)
                root.put("returned", returnedArray)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(root.toString().toByteArray())
                }

                _uiState.update { it.copy(message = "Copia de seguridad guardada correctamente ✓") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error al exportar: ${e.message}") }
            }
        }
    }

    fun importLoans(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val input = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                    ?: throw IllegalStateException("No se pudo leer el archivo")
                val root = JSONObject(input)
                var count = 0
                val arrays = listOf("active", "returned")
                arrays.forEach { key ->
                    if (root.has(key)) {
                        val arr = root.getJSONArray(key)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val loan = jsonToLoan(obj)
                            repository.insertLoan(loan)
                            count++
                        }
                    }
                }
                _uiState.update { it.copy(message = "Se han restaurado $count préstamos ✓") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error al importar: archivo no válido") }
            }
        }
    }

    fun deleteAllLoans() {
        viewModelScope.launch {
            try {
                val active = repository.getActiveLoans().first()
                val returned = repository.getReturnedLoans().first()
                (active + returned).forEach { repository.deleteLoan(it) }
                _uiState.update { it.copy(message = "Todos los datos han sido eliminados") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error al eliminar: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun loanToJson(loan: LoanEntity): JSONObject {
        val obj = JSONObject()
        obj.put("id", loan.id.toString())
        obj.put("nombreObjeto", loan.nombreObjeto)
        obj.put("contactoNombre", loan.contactoNombre)
        obj.put("contactoTelefono", loan.contactoTelefono)
        obj.put("contactoPhotoUri", loan.contactoPhotoUri)
        obj.put("fechaPrestamo", loan.fechaPrestamo.time)
        obj.put("fechaDevolucion", loan.fechaDevolucion.time)
        obj.put("fechaDevolucionReal", loan.fechaDevolucionReal?.time)
        obj.put("photoLoanUri", loan.photoLoanUri)
        obj.put("photoReturnUri", loan.photoReturnUri)
        obj.put("estado", loan.estado.name)
        obj.put("categoria", loan.categoria.name)
        obj.put("reminderCount", loan.reminderCount)
        obj.put("lastReminderTimestamp", loan.lastReminderTimestamp?.time)
        return obj
    }

    private fun jsonToLoan(obj: JSONObject): LoanEntity {
        val id = UUID.fromString(obj.getString("id"))
        val nombreObjeto = obj.getString("nombreObjeto")
        val contactoNombre = obj.getString("contactoNombre")
        val contactoTelefono = obj.getString("contactoTelefono")
        val contactoPhotoUri = if (obj.isNull("contactoPhotoUri")) null else obj.getString("contactoPhotoUri")
        val fechaPrestamo = Date(obj.getLong("fechaPrestamo"))
        val fechaDevolucion = Date(obj.getLong("fechaDevolucion"))
        val fechaDevolucionReal = if (obj.isNull("fechaDevolucionReal")) null else Date(obj.getLong("fechaDevolucionReal"))
        val photoLoanUri = if (obj.isNull("photoLoanUri")) null else obj.getString("photoLoanUri")
        val photoReturnUri = if (obj.isNull("photoReturnUri")) null else obj.getString("photoReturnUri")
        val estado = if (obj.has("estado")) LoanStatus.valueOf(obj.getString("estado")) else LoanStatus.ACTIVO
        val categoria = if (obj.has("categoria")) LoanCategory.valueOf(obj.getString("categoria")) else LoanCategory.OTROS
        val reminderCount = if (obj.has("reminderCount")) obj.getInt("reminderCount") else 0
        val lastReminderTimestamp = if (obj.isNull("lastReminderTimestamp")) null else Date(obj.getLong("lastReminderTimestamp"))

        return LoanEntity(
            id = id,
            nombreObjeto = nombreObjeto,
            contactoNombre = contactoNombre,
            contactoTelefono = contactoTelefono,
            contactoPhotoUri = contactoPhotoUri,
            fechaPrestamo = fechaPrestamo,
            fechaDevolucion = fechaDevolucion,
            fechaDevolucionReal = fechaDevolucionReal,
            photoLoanUri = photoLoanUri,
            photoReturnUri = photoReturnUri,
            estado = estado,
            categoria = categoria,
            reminderCount = reminderCount,
            lastReminderTimestamp = lastReminderTimestamp
        )
    }
}
