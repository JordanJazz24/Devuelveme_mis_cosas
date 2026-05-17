package com.example.devuelveme_mis_cosas.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.example.devuelveme_mis_cosas.domain.model.Contact
import com.example.devuelveme_mis_cosas.domain.repository.ContactsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import android.util.Log

class ContactsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ContactsRepository {

    override fun getContacts(): Flow<List<Contact>> = flow {
        val contactList = mutableListOf<Contact>()
        try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (it.moveToNext()) {
                    val id = if (idIndex >= 0) it.getString(idIndex) else ""
                    val name = if (nameIndex >= 0) it.getString(nameIndex) else "Sin nombre"
                    val number = if (numberIndex >= 0) it.getString(numberIndex) else null
                    val photoUri = if (photoIndex >= 0) it.getString(photoIndex) else null

                    if (name != null) {
                        contactList.add(Contact(id ?: "", name, number, photoUri))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ContactsRepo", "Error al leer contactos: ${e.message}")
        }
        emit(contactList)
    }.flowOn(Dispatchers.IO)
}
