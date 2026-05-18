package com.example.devuelveme_mis_cosas.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkerHelper {
    private const val DAILY_CHECK_WORK_NAME = "DailyCheckWork"

    fun schedulePeriodicWork(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<LoanReminderWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_CHECK_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
