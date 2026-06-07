package com.example.devuelveme_mis_cosas.data.repository

import com.example.devuelveme_mis_cosas.data.local.LoanDao
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class LoanRepositoryImpl @Inject constructor(
    private val loanDao: LoanDao
) : LoanRepository {
    override suspend fun insertLoan(loan: LoanEntity) = loanDao.insertLoan(loan)
    override suspend fun updateLoan(loan: LoanEntity) = loanDao.updateLoan(loan)
    override suspend fun deleteLoan(loan: LoanEntity) = loanDao.deleteLoan(loan)
    override fun getActiveLoans(): Flow<List<LoanEntity>> = loanDao.getActiveLoans()
    override fun getReturnedLoans(): Flow<List<LoanEntity>> = loanDao.getReturnedLoans()
    override fun getLoanById(id: UUID): Flow<LoanEntity> = loanDao.getLoanById(id)
    override fun getLoansForContact(phoneNumber: String): Flow<List<LoanEntity>> = loanDao.getLoansForContact(phoneNumber)
}
