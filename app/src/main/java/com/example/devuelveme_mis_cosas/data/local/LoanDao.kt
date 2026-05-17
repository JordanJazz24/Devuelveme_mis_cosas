package com.example.devuelveme_mis_cosas.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface LoanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity)

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    @Query("SELECT * FROM loans WHERE estado = 'ACTIVO' ORDER BY fechaDevolucion ASC")
    fun getActiveLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE estado = 'DEVUELTO' ORDER BY fechaDevolucionReal DESC")
    fun getReturnedLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :id")
    fun getLoanById(id: UUID): Flow<LoanEntity>
}
