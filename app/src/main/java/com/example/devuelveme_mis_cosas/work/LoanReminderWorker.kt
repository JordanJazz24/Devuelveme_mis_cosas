package com.example.devuelveme_mis_cosas.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.devuelveme_mis_cosas.MainActivity
import com.example.devuelveme_mis_cosas.R
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class LoanReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: LoanRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        // Sin cambios — se mantienen todas las constantes originales
        const val KEY_LOAN_ID = "loanId"
        const val KEY_CONTACTO_NOMBRE = "contactoNombre"
        const val KEY_NOMBRE_OBJETO = "nombreObjeto"
        const val CHANNEL_ID = "loan_reminders"
    }

    override suspend fun doWork(): Result {
        val activeLoans = repository.getActiveLoans().first()
        val now = Calendar.getInstance()

        activeLoans.forEach { loan ->
            val loanDueDate = Calendar.getInstance().apply {
                time = loan.fechaDevolucion
            }

            when (ReminderTriggerEvaluator.shouldSendReminder(loan, now.time)) {

                // Vence hoy: mensaje urgente 🔴
                ReminderType.DUE_TODAY ->
                    sendNotification(
                        loanId          = loan.id.toString(),
                        contactoNombre  = loan.contactoNombre,
                        nombreObjeto    = loan.nombreObjeto,
                        title           = "¡Hoy vence el préstamo! 🔴",
                        body            = "¿Ya te devolvió ${loan.contactoNombre} el/la ${loan.nombreObjeto}? ¡Vence hoy!"
                    )

                // Vence en 7 días: recordatorio anticipado 📦
                ReminderType.SEVEN_DAYS ->
                    sendNotification(
                        loanId          = loan.id.toString(),
                        contactoNombre  = loan.contactoNombre,
                        nombreObjeto    = loan.nombreObjeto,
                        title           = "Recordatorio de préstamo 📦",
                        body            = "¿Ya te devolvió ${loan.contactoNombre} el/la ${loan.nombreObjeto}? Vence en 7 días."
                    )

                // Ningún recordatorio aplica hoy
                ReminderType.NONE -> { /* no hacer nada */ }
            }
        }

        return Result.success()
    }

    // Firma idéntica a la original + parámetros opcionales title/body con
    // defaults para no romper ninguna llamada existente dentro del proyecto.
    private fun sendNotification(
        loanId: String,
        contactoNombre: String,
        nombreObjeto: String,
        title: String = "Recordatorio de préstamo 📦",
        body: String  = "¿Ya te devolvió $contactoNombre el/la $nombreObjeto? Vence pronto."
    ) {
        val context = applicationContext
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(KEY_LOAN_ID, loanId)
            action = Intent.ACTION_VIEW
        }

        val pendingIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(
                loanId.hashCode(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(loanId.hashCode(), notification)
    }
}
