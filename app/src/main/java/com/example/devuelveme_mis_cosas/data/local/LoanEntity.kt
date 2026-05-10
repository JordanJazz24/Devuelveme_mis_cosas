package com.example.devuelveme_mis_cosas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LoanStatus {
    ACTIVO,
    DEVUELTO
}

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombreObjeto: String,
    val contactoNombre: String,
    val contactoTelefono: String,
    val fechaPrestamo: Long,
    val fechaDevolucion: Long,
    val photoLoanUri: String?,
    val photoReturnUri: String?,
    val estado: LoanStatus
)
