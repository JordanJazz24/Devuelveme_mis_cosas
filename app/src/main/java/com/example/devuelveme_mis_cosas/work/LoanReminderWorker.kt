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
        const val KEY_LOAN_ID = "loanId"
        const val KEY_CONTACTO_NOMBRE = "contactoNombre"
        const val KEY_NOMBRE_OBJETO = "nombreObjeto"
        const val CHANNEL_ID = "loan_reminders"
    }

    override suspend fun doWork(): Result {
        val specificLoanId = inputData.getString(KEY_LOAN_ID)
        val specificContact = inputData.getString(KEY_CONTACTO_NOMBRE)
        val specificObject = inputData.getString(KEY_NOMBRE_OBJETO)

        if (specificLoanId != null && specificContact != null && specificObject != null) {
            // Es un recordatorio específico programado desde el ViewModel
            sendNotification(specificLoanId, specificContact, specificObject)
            return Result.success()
        }

        // Si no hay datos específicos, es el chequeo diario general
        // Buscamos préstamos que vencen en las próximas 24 horas
        val activeLoans = repository.getActiveLoans().first()
        val now = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }

        activeLoans.forEach { loan ->
            val loanDueDate = Calendar.getInstance().apply {
                time = loan.fechaDevolucion
            }

            if (loanDueDate.after(now) && loanDueDate.before(tomorrow)) {
                sendNotification(loan.id.toString(), loan.contactoNombre, loan.nombreObjeto)
            }
        }

        return Result.success()
    }

    private fun sendNotification(loanId: String, contactoNombre: String, nombreObjeto: String) {
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
            .setContentTitle("Recordatorio de préstamo 📦")
            .setContentText("¿Ya te devolvió $contactoNombre el/la $nombreObjeto? Vence pronto.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(loanId.hashCode(), notification)
    }
}
