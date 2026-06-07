package com.example.devuelveme_mis_cosas.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: LoanRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val groupedLoans: StateFlow<Map<String, List<LoanEntity>>> = combine(
        repository.getReturnedLoans(),
        _searchQuery
    ) { loans, query ->
        loans.filter { 
            it.nombreObjeto.contains(query, ignoreCase = true) || 
            it.contactoNombre.contains(query, ignoreCase = true) 
        }.groupBy { 
            getMonthYearString(it.fechaPrestamo)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch {
            repository.deleteAllReturnedLoans()
        }
    }

    private fun getMonthYearString(date: Date?): String {
        if (date == null) return "Sin fecha"
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(date).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
