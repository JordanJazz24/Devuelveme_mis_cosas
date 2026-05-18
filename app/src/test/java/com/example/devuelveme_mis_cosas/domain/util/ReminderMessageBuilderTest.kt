package com.example.devuelveme_mis_cosas.domain.util

import com.example.devuelveme_mis_cosas.data.local.LoanCategory
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ReminderMessageBuilderTest {

    private val testLoan = LoanEntity(
        id = UUID.randomUUID(),
        nombreObjeto = "El libro de Sapiens",
        contactoNombre = "Carlos",
        contactoTelefono = "88001122",
        contactoPhotoUri = null,
        fechaPrestamo = Date(),
        fechaDevolucion = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse("25/12/2025")!!,
        fechaDevolucionReal = null,
        photoLoanUri = null,
        photoReturnUri = null,
        estado = LoanStatus.ACTIVO,
        categoria = LoanCategory.LIBROS,
        reminderCount = 0,
        lastReminderTimestamp = null
    )

    @Test
    fun buildMessage_containsContactName() {
        val message = ReminderMessageBuilder.buildMessage(testLoan)
        assertTrue(message.contains("Carlos"))
    }

    @Test
    fun buildMessage_containsObjectName() {
        val message = ReminderMessageBuilder.buildMessage(testLoan)
        assertTrue(message.contains("El libro de Sapiens"))
    }

    @Test
    fun buildMessage_containsFormattedDate() {
        val message = ReminderMessageBuilder.buildMessage(testLoan)
        assertTrue(message.contains("25/12/2025"))
    }
}
