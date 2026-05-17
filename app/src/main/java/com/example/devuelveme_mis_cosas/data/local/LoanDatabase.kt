package com.example.devuelveme_mis_cosas.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [LoanEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LoanDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
}
