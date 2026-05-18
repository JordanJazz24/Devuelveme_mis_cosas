package com.example.devuelveme_mis_cosas.domain.util

import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import java.text.SimpleDateFormat
import java.util.*

object ReminderMessageBuilder {
    fun buildMessage(loan: LoanEntity): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(loan.fechaDevolucion)
        
        return "¡Hola ${loan.contactoNombre}! Te escribo para recordarte sobre el préstamo de: '${loan.nombreObjeto}'. Según mis notas, la fecha de devolución era el $dateStr. ¿Cuándo te vendría bien devolvérmelo? ¡Gracias!"
    }
}
