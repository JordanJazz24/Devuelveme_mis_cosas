package com.example.devuelveme_mis_cosas.work

import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import java.util.Calendar
import java.util.Date

/**
 * Tipos de recordatorio que puede producir la evaluación.
 */
enum class ReminderType {
    /** No corresponde enviar notificación. */
    NONE,

    /** El préstamo vence en exactamente 7 días calendario. */
    SEVEN_DAYS,

    /** El préstamo vence el día de hoy. */
    DUE_TODAY
}

/**
 * Evaluador puro de lógica de recordatorios.
 *
 * Esta clase NO tiene dependencias de Android. Recibe los datos del préstamo
 * y la fecha de referencia (normalmente `Date()` en producción) y devuelve
 * qué tipo de recordatorio aplica.
 *
 * Separar esta lógica del [LoanReminderWorker] permite:
 *  - Probarla exhaustivamente con unit tests JVM (sin emulador).
 *  - Reutilizarla en otros lugares de la app.
 *  - Mantener el Worker enfocado en la orquestación (obtener préstamos,
 *    llamar al evaluador, disparar la notificación).
 *
 * REGLAS:
 *  1. Si el préstamo está en estado DEVUELTO → NONE.
 *  2. Si la fecha de vencimiento es hoy (mismo día calendario) → DUE_TODAY.
 *  3. Si la fecha de vencimiento es exactamente 7 días calendario desde hoy → SEVEN_DAYS.
 *  4. Cualquier otro caso → NONE.
 */
object ReminderTriggerEvaluator {

    /**
     * Evalúa si se debe enviar una notificación para [loan].
     *
     * @param loan          El préstamo a evaluar.
     * @param referenceDate Fecha de referencia (normalmente [Date()] en producción).
     *                      Se inyecta para poder controlarla en tests.
     * @return El [ReminderType] que corresponde.
     */
    fun shouldSendReminder(loan: LoanEntity, referenceDate: Date): ReminderType {
        if (loan.estado == LoanStatus.DEVUELTO) return ReminderType.NONE

        val today = referenceDate.toCalendarDay()
        val dueDay = loan.fechaDevolucion.toCalendarDay()

        return when {
            dueDay == today     -> ReminderType.DUE_TODAY
            dueDay == today + 7 -> ReminderType.SEVEN_DAYS
            else                -> ReminderType.NONE
        }
    }

    /**
     * Convierte un [Date] al número de día del año ponderado por año
     * (year * 1000 + dayOfYear) para comparar fechas calendario sin
     * importar la hora del día ni el cruce de año.
     */
    private fun Date.toCalendarDay(): Int {
        val cal = Calendar.getInstance().apply { time = this@toCalendarDay }
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }
}
