package com.example.devuelveme_mis_cosas.presentation.navigation

sealed class Screen(val route: String) {
    object LoanList : Screen("loan_list")
    object NewLoan : Screen("new_loan")
    object LoanDetail : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: Int) = "loan_detail/$loanId"
    }
}
