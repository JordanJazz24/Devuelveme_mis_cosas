package com.example.devuelveme_mis_cosas.presentation.loan_detail

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: LoanDetailViewModel = hiltViewModel()
) {
    val loan by viewModel.loan.collectAsState()
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    // Usamos rememberSaveable para que la URI no se pierda al recrear la actividad (cámara)
    var tempPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUriString != null) {
            viewModel.markAsReturned(tempPhotoUriString)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Préstamo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            )
        }
    ) { padding ->
        loan?.let { entity ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Fotos
                Row(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    LoanPhoto(
                        uri = entity.photoLoanUri,
                        label = "Foto Inicial",
                        modifier = Modifier.weight(1f)
                    )
                    if (entity.estado == LoanStatus.DEVUELTO) {
                        Spacer(modifier = Modifier.width(8.dp))
                        LoanPhoto(
                            uri = entity.photoReturnUri,
                            label = "Foto Devolución",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = entity.nombreObjeto,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                AssistChip(
                    onClick = { },
                    label = { Text(entity.estado.name) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (entity.estado == LoanStatus.ACTIVO) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                )

                HorizontalDivider()

                DetailItem(label = "Contacto", value = entity.contactoNombre)
                if (entity.contactoTelefono.isNotBlank()) {
                    DetailItem(label = "Teléfono", value = entity.contactoTelefono)
                }
                DetailItem(label = "Categoría", value = entity.categoria.name)
                DetailItem(label = "Fecha de préstamo", value = dateFormatter.format(entity.fechaPrestamo))
                DetailItem(
                    label = if (entity.estado == LoanStatus.ACTIVO) "Fecha esperada" else "Fecha de devolución",
                    value = dateFormatter.format(entity.fechaDevolucion)
                )
                
                if (entity.reminderCount > 0) {
                    DetailItem(label = "Recordatorios enviados", value = entity.reminderCount.toString())
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (entity.estado == LoanStatus.ACTIVO) {
                    Button(
                        onClick = { viewModel.sendReminder(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Recordar por WhatsApp")
                    }

                    // Opción 1: Devolver sin foto (Nuevo requerimiento)
                    FilledTonalButton(
                        onClick = { viewModel.markAsReturned(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Marcar como Devuelto (Sin Foto)")
                    }

                    // Opción 2: Devolver con foto
                    OutlinedButton(
                        onClick = {
                            val photoFile = File(
                                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                "return_${System.currentTimeMillis()}.jpg"
                            )
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            tempPhotoUriString = uri.toString()
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Capturar Foto y Finalizar")
                    }
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Préstamo") },
            text = { Text("¿Estás seguro de que quieres eliminar este registro? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLoan()
                    onNavigateBack()
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun LoanPhoto(uri: String?, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = label,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Sin foto", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
