package com.example.devuelveme_mis_cosas.presentation.navigation

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.devuelveme_mis_cosas.presentation.history.HistoryScreen
import com.example.devuelveme_mis_cosas.presentation.loan_detail.LoanDetailScreen
import com.example.devuelveme_mis_cosas.presentation.loan_list.LoanListScreen
import com.example.devuelveme_mis_cosas.presentation.new_loan.NewLoanScreen
import com.example.devuelveme_mis_cosas.presentation.reputation.ReputationScreen
import com.example.devuelveme_mis_cosas.presentation.settings.SettingsScreen
import com.example.devuelveme_mis_cosas.work.LoanReminderWorker
import java.util.UUID

@Composable
fun MainScreen(
    intent: Intent?,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Manejo de Deep Link desde Notificación
    LaunchedEffect(intent) {
        intent?.getStringExtra(LoanReminderWorker.KEY_LOAN_ID)?.let { loanId ->
            navController.navigate(Screen.LoanDetail.createRoute(UUID.fromString(loanId)))
            intent.removeExtra(LoanReminderWorker.KEY_LOAN_ID)
        }
    }

    val bottomTabs = listOf(
        BottomNavTab(
            route = Screen.LoanList.route,
            icon = Icons.Default.List,
            label = "Préstamos"
        ),
        BottomNavTab(
            route = Screen.History.route,
            icon = Icons.Default.History,
            label = "Historial"
        ),
        BottomNavTab(
            route = Screen.Reputation.route,
            icon = Icons.Default.Star,
            label = "Reputación"
        )
    )

    Scaffold(
        bottomBar = {
            if (currentRoute == Screen.LoanList.route || 
                currentRoute == Screen.History.route || 
                currentRoute == Screen.Reputation.route) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    bottomTabs.forEach { tab ->
                        val isSelected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { 
                                Icon(
                                    imageVector = tab.icon, 
                                    contentDescription = tab.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            label = { 
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.LoanList.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.LoanList.route) {
                LoanListScreen(
                    onNavigateToNewLoan = { navController.navigate(Screen.NewLoan.route) },
                    onNavigateToDetail = { loanId -> navController.navigate(Screen.LoanDetail.createRoute(loanId)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.NewLoan.route) {
                NewLoanScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateToDetail = { loanId -> navController.navigate(Screen.LoanDetail.createRoute(loanId)) }
                )
            }
            composable(Screen.Reputation.route) {
                ReputationScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )
            }
            composable(
                route = Screen.LoanDetail.route,
                arguments = listOf(navArgument("loanId") { type = NavType.StringType })
            ) {
                LoanDetailScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

private data class BottomNavTab(
    val route: String,
    val icon: ImageVector,
    val label: String
)
