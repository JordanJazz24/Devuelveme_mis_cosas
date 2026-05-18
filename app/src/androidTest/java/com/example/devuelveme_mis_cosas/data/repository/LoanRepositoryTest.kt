package com.example.devuelveme_mis_cosas.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.devuelveme_mis_cosas.data.local.LoanDatabase
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import com.example.devuelveme_mis_cosas.data.local.LoanCategory
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LoanRepositoryTest {

    private lateinit var db: LoanDatabase
    private lateinit var repository: LoanRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LoanDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = LoanRepositoryImpl(db.loanDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertLoan_and_getActiveLoans_returnsInsertedLoan() = runTest {
        val loan = LoanEntity(
            id = UUID.randomUUID(),
            nombreObjeto = "Libro",
            contactoNombre = "Juan",
            contactoTelefono = "123456789",
            fechaDevolucion = Date(),
            photoLoanUri = null,
            estado = LoanStatus.ACTIVO,
            categoria = LoanCategory.LIBROS
        )
        repository.insertLoan(loan)

        val activeLoans = repository.getActiveLoans().first()
        assertEquals(1, activeLoans.size)
        assertEquals(loan.id, activeLoans[0].id)
    }

    @Test
    fun updateLoan_changesStatusToDevuelto_appearsInReturnedLoans() = runTest {
        val loan = LoanEntity(
            id = UUID.randomUUID(),
            nombreObjeto = "Libro",
            contactoNombre = "Juan",
            contactoTelefono = "123456789",
            fechaDevolucion = Date(),
            photoLoanUri = null,
            estado = LoanStatus.ACTIVO
        )
        repository.insertLoan(loan)

        val updatedLoan = loan.copy(estado = LoanStatus.DEVUELTO)
        repository.updateLoan(updatedLoan)

        val returnedLoans = repository.getReturnedLoans().first()
        assertEquals(1, returnedLoans.size)
        assertEquals(LoanStatus.DEVUELTO, returnedLoans[0].estado)
    }
}
