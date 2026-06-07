package com.example.devuelveme_mis_cosas.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showConfirmDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importLoans(context, it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportLoans(context, it) }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card {
                    ListItem(
                        headlineContent = { Text("Devuélveme mis cosas") },
                        supportingContent = { Text("Gestor inteligente de préstamos personales") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                        trailingContent = { Text("v1.2.0") }
                    )
                }
            }

            item {
                Card {
                    ListItem(
                        leadingContent = {
                            val icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode
                            Icon(icon, contentDescription = null)
                        },
                        headlineContent = { Text("Modo oscuro") },
                        trailingContent = {
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { onToggleDarkMode() }
                            )
                        }
                    )
                }
            }

            item {
                Card(onClick = {
                    exportLauncher.launch("devuelveme_backup_${System.currentTimeMillis()}.json")
                }) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Upload, contentDescription = null) },
                        headlineContent = { Text("Exportar préstamos") },
                        supportingContent = { Text("Guarda tus datos en un archivo JSON") }
                    )
                }
            }

            item {
                Card(onClick = {
                    importLauncher.launch(arrayOf("application/json"))
                }) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                        headlineContent = { Text("Importar préstamos") },
                        supportingContent = { Text("Restaura tus datos desde un archivo JSON") }
                    )
                }
            }

            item {
                Card(onClick = { showConfirmDialog = true }) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        headlineContent = {
                            Text(
                                "Borrar todos los datos",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        supportingContent = { Text("Elimina permanentemente todos los préstamos") }
                    )
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar borrado") },
            text = { Text("¿Estás seguro de eliminar todos los préstamos? Esta acción es irreversible.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    viewModel.deleteAllLoans()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
