package com.example.devuelveme_mis_cosas.presentation.reputation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.devuelveme_mis_cosas.data.local.ContactReputation
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.domain.repository.ContactReputationRepository
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReputationViewModel @Inject constructor(
    private val repository: ContactReputationRepository,
    private val loanRepository: LoanRepository
) : ViewModel() {

    val reputations: StateFlow<List<ContactReputation>> = repository.getAllOrderedByScore()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _contactLoans = MutableStateFlow<List<LoanEntity>>(emptyList())
    val contactLoans: StateFlow<List<LoanEntity>> = _contactLoans

    fun deleteReputation(reputation: ContactReputation) {
        viewModelScope.launch {
            repository.delete(reputation)
        }
    }

    private var fetchJob: Job? = null
    fun loadLoansForContact(phone: String) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            loanRepository.getLoansForContact(phone).collect { loans ->
                _contactLoans.value = loans
            }
        }
    }

    fun clearSelectedContact() {
        _contactLoans.value = emptyList()
    }
}
