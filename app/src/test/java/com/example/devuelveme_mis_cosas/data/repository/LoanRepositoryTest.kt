package com.example.devuelveme_mis_cosas.data.repository

import com.example.devuelveme_mis_cosas.data.local.LoanDao
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import com.example.devuelveme_mis_cosas.data.local.LoanCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID

class LoanRepositoryTest {

    private lateinit var repository: LoanRepositoryImpl
    private val dao: LoanDao = mockk()

    @Before
    fun setUp() {
        repository = LoanRepositoryImpl(dao)
    }

    @Test
    fun `insertLoan calls dao insertLoan`() = runTest {
        val loan = createTestLoan()
        coEvery { dao.insertLoan(any()) } returns Unit

        repository.insertLoan(loan)

        coVerify { dao.insertLoan(loan) }
    }

    @Test
    fun `getActiveLoans returns loans from dao`() = runTest {
        val loans = listOf(createTestLoan())
        every { dao.getActiveLoans() } returns flowOf(loans)

        repository.getActiveLoans().collect { result ->
            assert(result == loans)
        }
    }

    @Test
    fun `getLoanById calls dao getLoanById`() = runTest {
        val id = UUID.randomUUID()
        val loan = createTestLoan(id = id)
        every { dao.getLoanById(id) } returns flowOf(loan)

        repository.getLoanById(id).collect { result ->
            assert(result == loan)
        }
    }

    private fun createTestLoan(id: UUID = UUID.randomUUID()) = LoanEntity(
        id = id,
        nombreObjeto = "Test",
        contactoNombre = "Contact",
        contactoTelefono = "123",
        fechaPrestamo = Date(),
        fechaDevolucion = Date(),
        photoLoanUri = null,
        estado = LoanStatus.ACTIVO,
        categoria = LoanCategory.OTROS
    )
}
