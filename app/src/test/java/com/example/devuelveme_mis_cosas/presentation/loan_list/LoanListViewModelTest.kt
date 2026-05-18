package com.example.devuelveme_mis_cosas.presentation.loan_list

import app.cash.turbine.test
import com.example.devuelveme_mis_cosas.MainDispatcherRule
import com.example.devuelveme_mis_cosas.data.local.LoanCategory
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date
import java.util.UUID

class LoanListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: LoanListViewModel
    private lateinit var fakeRepository: FakeLoanRepository

    @Before
    fun setup() {
        fakeRepository = FakeLoanRepository()
        viewModel = LoanListViewModel(fakeRepository)
    }

    @Test
    fun activeLoans_initialValue_isEmpty() = runTest {
        viewModel.activeLoans.test {
            assertEquals(0, awaitItem().size)
        }
    }

    @Test
    fun activeLoans_whenRepositoryEmitsLoans_viewModelReflectsIt() = runTest {
        val loans = listOf(
            LoanEntity(
                id = UUID.randomUUID(),
                nombreObjeto = "Libro 1",
                contactoNombre = "Juan",
                contactoTelefono = "123456789",
                fechaDevolucion = Date(),
                photoLoanUri = null,
                estado = LoanStatus.ACTIVO
            )
        )
        
        viewModel.activeLoans.test {
            assertEquals(0, awaitItem().size) // Inicial
            fakeRepository.setLoans(loans)
            assertEquals(1, awaitItem().size) // Actualizado
        }
    }

    private class FakeLoanRepository : LoanRepository {
        private val loans = MutableStateFlow<List<LoanEntity>>(emptyList())
        fun setLoans(list: List<LoanEntity>) { loans.value = list }
        
        override fun getActiveLoans(): Flow<List<LoanEntity>> = loans
        override fun getReturnedLoans(): Flow<List<LoanEntity>> = flowOf(emptyList())
        override fun getLoanById(id: UUID): Flow<LoanEntity> = flowOf(
            LoanEntity(id, "Test", "Test", "", null, Date(), Date(), null, null, null, LoanStatus.ACTIVO, LoanCategory.OTROS, 0, null)
        )
        override suspend fun insertLoan(loan: LoanEntity) {}
        override suspend fun updateLoan(loan: LoanEntity) {}
        override suspend fun deleteLoan(loan: LoanEntity) {}
    }
}
