package com.example.devuelveme_mis_cosas.presentation.navigation

import java.util.UUID

sealed class Screen(val route: String) {
    object LoanList : Screen("loan_list")
    object NewLoan : Screen("new_loan")
    object History : Screen("history")
    object LoanDetail : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: UUID) = "loan_detail/$loanId"
    }
}
