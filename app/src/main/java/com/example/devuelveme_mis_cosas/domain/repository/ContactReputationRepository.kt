package com.example.devuelveme_mis_cosas.domain.repository

import com.example.devuelveme_mis_cosas.data.local.ContactReputation
import kotlinx.coroutines.flow.Flow

interface ContactReputationRepository {
    suspend fun upsert(reputation: ContactReputation)
    fun getByPhone(phone: String): Flow<ContactReputation?>
    fun getAllOrderedByScore(): Flow<List<ContactReputation>>
    suspend fun deleteAll()
}
