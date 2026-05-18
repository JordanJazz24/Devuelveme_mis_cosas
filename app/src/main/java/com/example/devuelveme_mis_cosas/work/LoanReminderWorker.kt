package com.example.devuelveme_mis_cosas.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.devuelveme_mis_cosas.MainActivity
import com.example.devuelveme_mis_cosas.R

class LoanReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_LOAN_ID = "loanId"
        const val KEY_CONTACTO_NOMBRE = "contactoNombre"
        const val KEY_NOMBRE_OBJETO = "nombreObjeto"
        const val CHANNEL_ID = "loan_reminders"
    }

    override suspend fun doWork(): Result {
        val loanId = inputData.getString(KEY_LOAN_ID) ?: return Result.failure()
        val contactoNombre = inputData.getString(KEY_CONTACTO_NOMBRE) ?: ""
        val nombreObjeto = inputData.getString(KEY_NOMBRE_OBJETO) ?: ""

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
            .setSmallIcon(R.drawable.ic_notification) // Asegúrate de tener este recurso
            .setContentTitle("Recordatorio de préstamo 📦")
            .setContentText("¿Ya te devolvió $contactoNombre el/la $nombreObjeto?")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(loanId.hashCode(), notification)

        return Result.success()
    }
}

