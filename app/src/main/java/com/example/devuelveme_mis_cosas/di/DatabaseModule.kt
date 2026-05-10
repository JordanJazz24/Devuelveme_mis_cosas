package com.example.devuelveme_mis_cosas.di

import android.content.Context
import androidx.room.Room
import com.example.devuelveme_mis_cosas.data.local.LoanDao
import com.example.devuelveme_mis_cosas.data.local.LoanDatabase
import com.example.devuelveme_mis_cosas.data.repository.LoanRepositoryImpl
import com.example.devuelveme_mis_cosas.domain.repository.LoanRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLoanDatabase(@ApplicationContext context: Context): LoanDatabase {
        return Room.databaseBuilder(
            context,
            LoanDatabase::class.java,
            "loans_db"
        ).build()
    }

    @Provides
    fun provideLoanDao(database: LoanDatabase): LoanDao {
        return database.loanDao()
    }

    @Provides
    @Singleton
    fun provideLoanRepository(loanDao: LoanDao): LoanRepository {
        return LoanRepositoryImpl(loanDao)
    }
}
