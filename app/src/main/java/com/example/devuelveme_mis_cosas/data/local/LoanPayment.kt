package com.example.devuelveme_mis_cosas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "loan_payments")
data class LoanPayment(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val loanId: UUID,
    val amount: Double,
    val paymentDate: Date = Date(),
    val note: String? = null
)
