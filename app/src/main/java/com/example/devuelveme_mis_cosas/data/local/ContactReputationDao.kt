package com.example.devuelveme_mis_cosas.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactReputationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reputation: ContactReputation)

    @Query("SELECT * FROM contact_reputation WHERE contactPhone = :phone")
    fun getByPhone(phone: String): Flow<ContactReputation?>

    @Query("SELECT * FROM contact_reputation ORDER BY reputationScore DESC")
    fun getAllOrderedByScore(): Flow<List<ContactReputation>>

    @Query("DELETE FROM contact_reputation")
    suspend fun deleteAll()
}
