package com.example.devuelveme_mis_cosas.work

import com.example.devuelveme_mis_cosas.data.local.LoanCategory
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.UUID

/**
 * Unit tests para la lógica de notificaciones del [LoanReminderWorker].
 */
class LoanReminderWorkerTest {

    // ─── Helper para construir préstamos de prueba ────────────────────────────

    /**
     * Crea un [LoanEntity] de prueba con [fechaDevolucion] configurable.
     * Por defecto está ACTIVO; se puede pasar [estado] = DEVUELTO.
     */
    private fun buildLoan(
        fechaDevolucion: Date,
        estado: LoanStatus = LoanStatus.ACTIVO
    ): LoanEntity = LoanEntity(
        id = UUID.randomUUID(),
        nombreObjeto = "Libro de Sapiens",
        contactoNombre = "Carlos",
        contactoTelefono = "88001122",
        fechaPrestamo = Date(),
        fechaDevolucion = fechaDevolucion,
        photoLoanUri = null,
        estado = estado,
        categoria = LoanCategory.LIBROS
    )

    /** Devuelve un [Date] que representa "hoy" más [days] días.
     *  Con [days] = 0 → hoy; [days] = -1 → ayer; [days] = 7 → en 7 días. */
    private fun today(plusDays: Int = 0): Date =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, plusDays)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

    // ─── Tests para ReminderType ──────────────────────────────────────────────

    /**
     * Préstamo que vence exactamente en 7 días:
     * debe devolver [ReminderType.SEVEN_DAYS].
     */
    @Test
    fun `loan due in exactly 7 days returns SEVEN_DAYS reminder`() {
        val loan = buildLoan(fechaDevolucion = today(plusDays = 7))
        val result = ReminderTriggerEvaluator.shouldSendReminder(loan, referenceDate = today())
        assertTrue(
            "Se esperaba SEVEN_DAYS pero fue $result",
            result == ReminderType.SEVEN_DAYS
        )
    }

    /**
     * Préstamo que vence hoy:
     * debe devolver [ReminderType.DUE_TODAY].
     */
    @Test
    fun `loan due today returns DUE_TODAY reminder`() {
        val loan = buildLoan(fechaDevolucion = today())
        val result = ReminderTriggerEvaluator.shouldSendReminder(loan, referenceDate = today())
        assertTrue(
            "Se esperaba DUE_TODAY pero fue $result",
            result == ReminderType.DUE_TODAY
        )
    }

    /**
     * Préstamo con 8 días restantes:
     * no aplica ninguna notificación → [ReminderType.NONE].
     */
    @Test
    fun `loan due in 8 days returns NONE reminder`() {
        val loan = buildLoan(fechaDevolucion = today(plusDays = 8))
        val result = ReminderTriggerEvaluator.shouldSendReminder(loan, referenceDate = today())
        assertTrue(
            "Se esperaba NONE pero fue $result",
            result == ReminderType.NONE
        )
    }

    /**
     * Préstamo con 6 días restantes (no es ni 7 días ni hoy):
     * no aplica ninguna notificación → [ReminderType.NONE].
     */
    @Test
    fun `loan due in 6 days returns NONE reminder`() {
        val loan = buildLoan(fechaDevolucion = today(plusDays = 6))
        val result = ReminderTriggerEvaluator.shouldSendReminder(loan, referenceDate = today())
        assertTrue(
            "Se esperaba NONE pero fue $result",
            result == ReminderType.NONE
        )
    }

    /**
     * Préstamo vencido (venció ayer):
     * no aplica ninguna notificación → [ReminderType.NONE].
     */
    @Test
    fun `overdue loan (yesterday) returns NONE reminder`() {
        val loan = buildLoan(fechaDevolucion = today(plusDays = -1))
        val result = ReminderTriggerEvaluator.shouldSendReminder(loan, referenceDate = today())
        assertTrue(
            "Un préstamo vencido no debería generar notificación nueva, pero fue $result",
            result == ReminderType.NONE
        )
    }

    /**
     * Préstamo ya devuelto (estado DEVUELTO) aunque venza hoy:
     * no aplica ninguna notificación → [ReminderType.NONE].
     */
    @Test
    fun `returned loan is never notified even if due today`() {
        val loan = buildLoan(
            fechaDevolucion = today(),
            estado = LoanStatus.DEVUELTO
        )
        val result = ReminderTriggerEvaluator.shouldSendReminder(loan, referenceDate = today())
        assertTrue(
            "Un préstamo DEVUELTO nunca debe notificarse, pero fue $result",
            result == ReminderType.NONE
        )
    }

    /**
     * Préstamo que vence en 7 días pero a las 23:59:59:
     * debe seguir siendo [ReminderType.SEVEN_DAYS] (misma fecha calendario).
     */
    @Test
    fun `loan due in 7 days at end of day still returns SEVEN_DAYS`() {
        val dueDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.time
        val loan = buildLoan(fechaDevolucion = dueDate)
        val result = ReminderTriggerEvaluator.shouldSendReminder(loan, referenceDate = today())
        assertTrue(
            "Aunque sea tarde en el día, 7 días sigue siendo SEVEN_DAYS. Fue $result",
            result == ReminderType.SEVEN_DAYS
        )
    }

    /**
     * Lista mixta de préstamos:
     * solo los que vencen hoy o en 7 días exactos deben ser notificados.
     */
    @Test
    fun `mixed list only notifies loans due today or in 7 days`() {
        val loans = listOf(
            buildLoan(today()),           // DUE_TODAY   ✓
            buildLoan(today(7)),          // SEVEN_DAYS  ✓
            buildLoan(today(8)),          // NONE        ✗
            buildLoan(today(6)),          // NONE        ✗
            buildLoan(today(-1)),         // NONE        ✗ (vencido)
            buildLoan(today(1)),          // NONE        ✗
            buildLoan(today(14)),         // NONE        ✗
        )

        val now = today()
        val toNotify = loans.filter {
            ReminderTriggerEvaluator.shouldSendReminder(it, now) != ReminderType.NONE
        }

        assertTrue(
            "Se esperaban exactamente 2 préstamos para notificar, pero fueron ${toNotify.size}",
            toNotify.size == 2
        )

        val resultTypes = toNotify.map { ReminderTriggerEvaluator.shouldSendReminder(it, now) }.toSet()
        assertTrue("Debe haber un DUE_TODAY", resultTypes.contains(ReminderType.DUE_TODAY))
        assertTrue("Debe haber un SEVEN_DAYS", resultTypes.contains(ReminderType.SEVEN_DAYS))
    }

    /**
     * Préstamo que vence mañana (en 1 día): no es ni hoy ni 7 días → NONE.
     */
    @Test
    fun `loan due tomorrow is not notified`() {
        val loan = buildLoan(today(plusDays = 1))
        val result = ReminderTriggerEvaluator.shouldSendReminder(loan, referenceDate = today())
        assertFalse(
            "Un préstamo que vence mañana no debe notificarse",
            result != ReminderType.NONE
        )
    }

    /**
     * Préstamo que vence en 7 días con estado DEVUELTO → no notifica.
     */
    @Test
    fun `returned loan due in 7 days is not notified`() {
        val loan = buildLoan(today(plusDays = 7), estado = LoanStatus.DEVUELTO)
        val result = ReminderTriggerEvaluator.shouldSendReminder(loan, referenceDate = today())
        assertTrue(
            "Un préstamo DEVUELTO con 7 días no debe notificarse, fue $result",
            result == ReminderType.NONE
        )
    }
}
