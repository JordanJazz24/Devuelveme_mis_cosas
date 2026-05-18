package com.example.devuelveme_mis_cosas

import android.app.Application
import com.example.devuelveme_mis_cosas.work.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DevuelvemeMisCosasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
