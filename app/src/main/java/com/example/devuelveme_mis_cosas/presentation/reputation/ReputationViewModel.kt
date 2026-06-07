package com.example.devuelveme_mis_cosas.presentation.reputation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.devuelveme_mis_cosas.data.local.ContactReputation
import com.example.devuelveme_mis_cosas.domain.repository.ContactReputationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReputationViewModel @Inject constructor(
    private val repository: ContactReputationRepository
) : ViewModel() {

    val reputations: StateFlow<List<ContactReputation>> = repository.getAllOrderedByScore()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteReputation(reputation: ContactReputation) {
        viewModelScope.launch {
            repository.delete(reputation)
        }
    }
}
