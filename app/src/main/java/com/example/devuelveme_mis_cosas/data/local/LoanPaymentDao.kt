package com.example.devuelveme_mis_cosas.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface LoanPaymentDao {
    @Insert
    suspend fun insertPayment(payment: LoanPayment)

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY paymentDate DESC")
    fun getPaymentsByLoanId(loanId: UUID): Flow<List<LoanPayment>>

    @Query("DELETE FROM loan_payments WHERE loanId = :loanId")
    suspend fun deletePaymentsByLoanId(loanId: UUID)
}
