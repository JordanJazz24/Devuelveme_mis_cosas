package com.example.devuelveme_mis_cosas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.devuelveme_mis_cosas.presentation.history.HistoryScreen
import com.example.devuelveme_mis_cosas.presentation.loan_detail.LoanDetailScreen
import com.example.devuelveme_mis_cosas.presentation.loan_list.LoanListScreen
import com.example.devuelveme_mis_cosas.presentation.navigation.Screen
import com.example.devuelveme_mis_cosas.presentation.new_loan.NewLoanScreen
import com.example.devuelveme_mis_cosas.ui.theme.Devuelveme_mis_cosasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Devuelveme_mis_cosasTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.LoanList.route
    ) {
        composable(Screen.LoanList.route) {
            LoanListScreen(
                onNavigateToNewLoan = {
                    navController.navigate(Screen.NewLoan.route)
                },
                onNavigateToDetail = { loanId ->
                    navController.navigate(Screen.LoanDetail.createRoute(loanId))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                }
            )
        }
        composable(Screen.NewLoan.route) {
            NewLoanScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { loanId ->
                    navController.navigate(Screen.LoanDetail.createRoute(loanId))
                }
            )
        }
        composable(
            route = Screen.LoanDetail.route,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) {
            LoanDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
