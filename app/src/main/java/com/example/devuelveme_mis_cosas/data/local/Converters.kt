package com.example.devuelveme_mis_cosas.data.local

import androidx.room.TypeConverter
import java.util.Date
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromUUID(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun toUUID(value: String?): UUID? = value?.let { UUID.fromString(it) }

    @TypeConverter
    fun fromStatus(status: LoanStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): LoanStatus = LoanStatus.valueOf(value)

    @TypeConverter
    fun fromCategory(category: LoanCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): LoanCategory = LoanCategory.valueOf(value)
}
