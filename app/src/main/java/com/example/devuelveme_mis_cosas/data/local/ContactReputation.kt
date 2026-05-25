package com.example.devuelveme_mis_cosas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_reputation")
data class ContactReputation(
    @PrimaryKey val contactPhone: String,
    val contactName: String,
    val contactPhotoUri: String? = null,
    val totalLoans: Int = 0,
    val returnedOnTime: Int = 0,
    val returnedLate: Int = 0,
    val returnedDamaged: Int = 0,
    val neverReturned: Int = 0,
    val reputationScore: Float = 0f  // calculado, no persistir lógica aquí
)

