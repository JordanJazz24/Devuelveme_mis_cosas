package com.example.devuelveme_mis_cosas

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.devuelveme_mis_cosas.presentation.navigation.Screen
import com.example.devuelveme_mis_cosas.presentation.new_loan.NewLoanScreen
import com.example.devuelveme_mis_cosas.presentation.reputation.ReputationScreen
import com.example.devuelveme_mis_cosas.presentation.settings.SettingsScreen
import com.example.devuelveme_mis_cosas.ui.theme.Devuelveme_mis_cosasTheme
import com.example.devuelveme_mis_cosas.work.LoanReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val intentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentState.value = intent
        enableEdgeToEdge()
        setContent {
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            var isDarkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }

            val onToggleDarkMode = {
                val newValue = !isDarkMode
                isDarkMode = newValue
                prefs.edit().putBoolean("dark_mode", isDarkMode).apply()
            }

            Devuelveme_mis_cosasTheme(darkTheme = isDarkMode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val currentIntent by intentState
                AppNavigation(
                    intent = currentIntent,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentState.value = intent
    }
}

@Composable
fun AppNavigation(
    intent: Intent?,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(intent) {
        intent?.getStringExtra(LoanReminderWorker.KEY_LOAN_ID)?.let { loanId ->
            navController.navigate(Screen.LoanDetail.createRoute(UUID.fromString(loanId)))
            intent.removeExtra(LoanReminderWorker.KEY_LOAN_ID)
        }
    }

    val bottomTabs = listOf(
        BottomNavTab(Screen.LoanList.route, Icons.Default.List, "Préstamos"),
        BottomNavTab(Screen.History.route, Icons.Default.History, "Historial"),
        BottomNavTab(Screen.Reputation.route, Icons.Default.Star, "Reputación")
    )

    Scaffold(
        bottomBar = {
            if (currentRoute == Screen.LoanList.route || 
                currentRoute == Screen.History.route || 
                currentRoute == Screen.Reputation.route) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { androidx.compose.material3.Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
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

private data class BottomNavTab(val route: String, val icon: ImageVector, val label: String)
