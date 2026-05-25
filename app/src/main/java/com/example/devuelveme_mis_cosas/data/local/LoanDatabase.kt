package com.example.devuelveme_mis_cosas.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LoanEntity::class, ContactReputation::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LoanDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
    abstract fun contactReputationDao(): ContactReputationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loans ADD COLUMN notes TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add returnCondition column to loans table
                db.execSQL("ALTER TABLE loans ADD COLUMN returnCondition TEXT")

                // Create contact_reputation table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS contact_reputation (
                        contactPhone TEXT PRIMARY KEY NOT NULL,
                        contactName TEXT NOT NULL,
                        contactPhotoUri TEXT,
                        totalLoans INTEGER NOT NULL DEFAULT 0,
                        returnedOnTime INTEGER NOT NULL DEFAULT 0,
                        returnedLate INTEGER NOT NULL DEFAULT 0,
                        returnedDamaged INTEGER NOT NULL DEFAULT 0,
                        neverReturned INTEGER NOT NULL DEFAULT 0,
                        reputationScore REAL NOT NULL DEFAULT 0.0
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
