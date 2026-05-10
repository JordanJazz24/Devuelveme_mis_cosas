package com.example.devuelveme_mis_cosas.domain.repository

import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    suspend fun insertLoan(loan: LoanEntity)
    suspend fun updateLoan(loan: LoanEntity)
    suspend fun deleteLoan(loan: LoanEntity)
    fun getActiveLoans(): Flow<List<LoanEntity>>
    fun getReturnedLoans(): Flow<List<LoanEntity>>
    fun getLoanById(id: Int): Flow<LoanEntity>
}
