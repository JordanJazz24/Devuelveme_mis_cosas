package com.example.devuelveme_mis_cosas.domain.repository

import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface LoanRepository {
    suspend fun insertLoan(loan: LoanEntity)
    suspend fun updateLoan(loan: LoanEntity)
    suspend fun deleteLoan(loan: LoanEntity)
    fun getActiveLoans(): Flow<List<LoanEntity>>
    fun getReturnedLoans(): Flow<List<LoanEntity>>
    fun getLoanById(id: UUID): Flow<LoanEntity>
    fun getLoansForContact(phoneNumber: String): Flow<List<LoanEntity>>
}
