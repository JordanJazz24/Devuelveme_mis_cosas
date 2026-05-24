package com.example.devuelveme_mis_cosas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

enum class LoanStatus {
    ACTIVO,
    DEVUELTO
}

enum class LoanCategory {
    DINERO,
    HERRAMIENTAS,
    LIBROS,
    ELECTRONICA,
    ROPA,
    VIDEOJUEGOS,
    PELICULAS,
    OTROS
}

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val nombreObjeto: String,
    val contactoNombre: String,
    val contactoTelefono: String,
    val contactoPhotoUri: String? = null,
    val fechaPrestamo: Date = Date(),
    val fechaDevolucion: Date, // Fecha esperada
    val fechaDevolucionReal: Date? = null, // Fecha en la que se devolvió
    val photoLoanUri: String?,
    val photoReturnUri: String? = null,
    val estado: LoanStatus = LoanStatus.ACTIVO,
    val categoria: LoanCategory = LoanCategory.OTROS,
    val reminderCount: Int = 0,
    val lastReminderTimestamp: Date? = null,
    val notes: String? = null
)
