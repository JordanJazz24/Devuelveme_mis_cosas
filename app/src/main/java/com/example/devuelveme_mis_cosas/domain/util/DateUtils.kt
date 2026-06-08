package com.example.devuelveme_mis_cosas.domain.util

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object DateUtils {
    fun normalizeDateToLocalMidday(millis: Long): Date {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.timeInMillis = millis
        
        val result = Calendar.getInstance()
        result.set(
            utcCalendar.get(Calendar.YEAR),
            utcCalendar.get(Calendar.MONTH),
            utcCalendar.get(Calendar.DAY_OF_MONTH),
            12, 0, 0
        )
        result.set(Calendar.MILLISECOND, 0)
        return result.time
    }

    fun getUtcMillis(date: Date): Long {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.set(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
        utcCalendar.set(Calendar.MILLISECOND, 0)
        return utcCalendar.timeInMillis
    }
}
