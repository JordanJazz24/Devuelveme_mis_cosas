package com.example.devuelveme_mis_cosas.data.repository

import com.example.devuelveme_mis_cosas.data.local.ContactReputation
import com.example.devuelveme_mis_cosas.data.local.ContactReputationDao
import com.example.devuelveme_mis_cosas.domain.repository.ContactReputationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ContactReputationRepositoryImpl @Inject constructor(
    private val dao: ContactReputationDao
) : ContactReputationRepository {

    override suspend fun upsert(reputation: ContactReputation) {
        dao.upsert(reputation)
    }

    override fun getByPhone(phone: String): Flow<ContactReputation?> {
        return dao.getByPhone(phone)
    }

    override fun getAllOrderedByScore(): Flow<List<ContactReputation>> {
        return dao.getAllOrderedByScore()
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}
