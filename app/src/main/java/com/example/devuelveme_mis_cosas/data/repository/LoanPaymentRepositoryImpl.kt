package com.example.devuelveme_mis_cosas.data.repository

import com.example.devuelveme_mis_cosas.data.local.LoanPayment
import com.example.devuelveme_mis_cosas.data.local.LoanPaymentDao
import com.example.devuelveme_mis_cosas.domain.repository.LoanPaymentRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class LoanPaymentRepositoryImpl @Inject constructor(
    private val loanPaymentDao: LoanPaymentDao
) : LoanPaymentRepository {
    override suspend fun insertPayment(payment: LoanPayment) {
        loanPaymentDao.insertPayment(payment)
    }

    override fun getPaymentsByLoanId(loanId: UUID): Flow<List<LoanPayment>> {
        return loanPaymentDao.getPaymentsByLoanId(loanId)
    }

    override suspend fun deletePaymentsByLoanId(loanId: UUID) {
        loanPaymentDao.deletePaymentsByLoanId(loanId)
    }
}
