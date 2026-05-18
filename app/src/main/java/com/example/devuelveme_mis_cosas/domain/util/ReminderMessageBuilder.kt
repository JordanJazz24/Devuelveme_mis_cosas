package com.example.devuelveme_mis_cosas.domain.util

import com.example.devuelveme_mis_cosas.data.local.LoanEntity

object ReminderMessageBuilder {
    fun buildMessage(loan: LoanEntity): String {
        return "¡Hola ${loan.contactoNombre}! Espero que estés muy bien. Te escribía para recordarte sobre '${loan.nombreObjeto}' que te presté. ¿Me avisas cuando puedas devolvérmelo? ¡Gracias!"
    }
}
