package com.example.devuelveme_mis_cosas.domain.repository

import com.example.devuelveme_mis_cosas.domain.model.Contact
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {
    fun getContacts(): Flow<List<Contact>>
}
