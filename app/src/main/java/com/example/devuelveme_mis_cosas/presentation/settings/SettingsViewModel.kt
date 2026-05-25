package com.example.devuelveme_mis_cosas.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.devuelveme_mis_cosas.data.local.ContactReputation
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import com.example.devuelveme_mis_cosas.data.local.LoanCategory
import com.example.devuelveme_mis_cosas.domain.repository.ContactReputationRepository
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
    private val repository: LoanRepository,
    private val reputationRepository: ContactReputationRepository
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
                val reputations = reputationRepository.getAllOrderedByScore().first()

                val root = JSONObject()
                
                // Exportar Préstamos
                val loansArray = JSONArray()
                (active + returned).forEach { loansArray.put(loanToJson(it)) }
                root.put("loans", loansArray)

                // Exportar Reputaciones (Solución a tu problema)
                val repArray = JSONArray()
                reputations.forEach { repArray.put(reputationToJson(it)) }
                root.put("reputations", repArray)

                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(root.toString().toByteArray())
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
                
                var loanCount = 0
                if (root.has("loans")) {
                    val arr = root.getJSONArray("loans")
                    for (i in 0 until arr.length()) {
                        repository.insertLoan(jsonToLoan(arr.getJSONObject(i)))
                        loanCount++
                    }
                }

                if (root.has("reputations")) {
                    val arr = root.getJSONArray("reputations")
                    for (i in 0 until arr.length()) {
                        reputationRepository.upsert(jsonToReputation(arr.getJSONObject(i)))
                    }
                }

                _uiState.update { it.copy(message = "Se han restaurado los préstamos y reputaciones ✓") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error al importar archivo") }
            }
        }
    }

    fun deleteAllLoans() {
        viewModelScope.launch {
            try {
                // Borrar préstamos
                val allLoans = repository.getActiveLoans().first() + repository.getReturnedLoans().first()
                allLoans.forEach { repository.deleteLoan(it) }
                
                // Borrar reputaciones (Solución a tu problema)
                reputationRepository.deleteAll()
                
                _uiState.update { it.copy(message = "Todos los datos han sido eliminados") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error al eliminar datos") }
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
        obj.put("contactoPhotoUri", loan.contactoPhotoUri ?: JSONObject.NULL)
        obj.put("fechaPrestamo", loan.fechaPrestamo.time)
        obj.put("fechaDevolucion", loan.fechaDevolucion.time)
        obj.put("fechaDevolucionReal", loan.fechaDevolucionReal?.time ?: JSONObject.NULL)
        obj.put("photoLoanUri", loan.photoLoanUri ?: JSONObject.NULL)
        obj.put("photoReturnUri", loan.photoReturnUri ?: JSONObject.NULL)
        obj.put("estado", loan.estado.name)
        obj.put("categoria", loan.categoria.name)
        obj.put("reminderCount", loan.reminderCount)
        obj.put("lastReminderTimestamp", loan.lastReminderTimestamp?.time ?: JSONObject.NULL)
        obj.put("notes", loan.notes ?: JSONObject.NULL)
        obj.put("returnCondition", loan.returnCondition ?: JSONObject.NULL)
        return obj
    }

    private fun jsonToLoan(obj: JSONObject): LoanEntity {
        return LoanEntity(
            id = UUID.fromString(obj.getString("id")),
            nombreObjeto = obj.getString("nombreObjeto"),
            contactoNombre = obj.getString("contactoNombre"),
            contactoTelefono = obj.getString("contactoTelefono"),
            contactoPhotoUri = if (obj.isNull("contactoPhotoUri")) null else obj.getString("contactoPhotoUri"),
            fechaPrestamo = Date(obj.getLong("fechaPrestamo")),
            fechaDevolucion = Date(obj.getLong("fechaDevolucion")),
            fechaDevolucionReal = if (obj.isNull("fechaDevolucionReal")) null else Date(obj.getLong("fechaDevolucionReal")),
            photoLoanUri = if (obj.isNull("photoLoanUri")) null else obj.getString("photoLoanUri"),
            photoReturnUri = if (obj.isNull("photoReturnUri")) null else obj.getString("photoReturnUri"),
            estado = LoanStatus.valueOf(obj.optString("estado", "ACTIVO")),
            categoria = LoanCategory.valueOf(obj.optString("categoria", "OTROS")),
            reminderCount = obj.optInt("reminderCount", 0),
            lastReminderTimestamp = if (obj.isNull("lastReminderTimestamp")) null else Date(obj.getLong("lastReminderTimestamp")),
            notes = if (obj.isNull("notes")) null else obj.getString("notes"),
            returnCondition = if (obj.isNull("returnCondition")) null else obj.getString("returnCondition")
        )
    }

    private fun reputationToJson(rep: ContactReputation): JSONObject {
        val obj = JSONObject()
        obj.put("contactPhone", rep.contactPhone)
        obj.put("contactName", rep.contactName)
        obj.put("contactPhotoUri", rep.contactPhotoUri ?: JSONObject.NULL)
        obj.put("totalLoans", rep.totalLoans)
        obj.put("returnedOnTime", rep.returnedOnTime)
        obj.put("returnedLate", rep.returnedLate)
        obj.put("returnedDamaged", rep.returnedDamaged)
        obj.put("neverReturned", rep.neverReturned)
        obj.put("reputationScore", rep.reputationScore.toDouble())
        return obj
    }

    private fun jsonToReputation(obj: JSONObject): ContactReputation {
        return ContactReputation(
            contactPhone = obj.getString("contactPhone"),
            contactName = obj.getString("contactName"),
            contactPhotoUri = if (obj.isNull("contactPhotoUri")) null else obj.getString("contactPhotoUri"),
            totalLoans = obj.getInt("totalLoans"),
            returnedOnTime = obj.getInt("returnedOnTime"),
            returnedLate = obj.getInt("returnedLate"),
            returnedDamaged = obj.getInt("returnedDamaged"),
            neverReturned = obj.getInt("neverReturned"),
            reputationScore = obj.getDouble("reputationScore").toFloat()
        )
    }
}
