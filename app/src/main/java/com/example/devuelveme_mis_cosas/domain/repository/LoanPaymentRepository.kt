package com.example.devuelveme_mis_cosas.domain.repository

import com.example.devuelveme_mis_cosas.data.local.LoanPayment
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface LoanPaymentRepository {
    suspend fun insertPayment(payment: LoanPayment)
    fun getPaymentsByLoanId(loanId: UUID): Flow<List<LoanPayment>>
    suspend fun deletePaymentsByLoanId(loanId: UUID)
}
