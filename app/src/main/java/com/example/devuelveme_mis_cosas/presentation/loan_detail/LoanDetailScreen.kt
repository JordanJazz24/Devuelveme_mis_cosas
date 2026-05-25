package com.example.devuelveme_mis_cosas.presentation.loan_detail

import android.Manifest
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val snackbarHostState = remember { SnackbarHostState() }
    
    var tempPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReturnConditionSheet by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUriString != null) {
            showReturnConditionSheet = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val photoFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "return_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                tempPhotoUriString = uri.toString()
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Error al preparar la cámara", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.reminderMessage) {
        uiState.reminderMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearReminderMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

                Column {
                    Text(
                        text = entity.nombreObjeto,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                        // PROBLEMA 1: Mostrar condición de devolución
                        if (entity.estado == LoanStatus.DEVUELTO && entity.returnCondition != null) {
                            val conditionColor = when(entity.returnCondition) {
                                "EXCELENTE" -> Color(0xFF27AE60)
                                "BUENO" -> Color(0xFF2980B9)
                                "MALO" -> Color(0xFFE67E22)
                                "NUNCA_DEVUELTO" -> Color(0xFFC0392B)
                                else -> MaterialTheme.colorScheme.secondary
                            }
                            
                            AssistChip(
                                onClick = { },
                                label = { Text(entity.returnCondition) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = conditionColor.copy(alpha = 0.1f),
                                    labelColor = conditionColor
                                )
                            )
                        }
                    }
                }

                HorizontalDivider()

                DetailItem(label = "Contacto", value = entity.contactoNombre)
                if (entity.contactoTelefono.isNotBlank()) {
                    DetailItem(label = "Teléfono", value = entity.contactoTelefono)
                }
                DetailItem(label = "Categoría", value = entity.categoria.name)
                DetailItem(label = "Fecha de préstamo", value = dateFormatter.format(entity.fechaPrestamo))
                
                // PROBLEMA 2: Mostrar fecha acordada vs fecha real
                DetailItem(label = "Fecha límite acordada", value = dateFormatter.format(entity.fechaDevolucion))
                
                if (entity.fechaDevolucionReal != null) {
                    DetailItem(label = "Fecha de devolución real", value = dateFormatter.format(entity.fechaDevolucionReal))
                }

                if (!entity.notes.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Notas",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entity.notes,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                if (entity.reminderCount > 0) {
                    DetailItem(label = "Recordatorios enviados", value = entity.reminderCount.toString())
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (entity.estado == LoanStatus.ACTIVO) {
                    Button(
                        onClick = { viewModel.sendReminder() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Recordar por WhatsApp")
                    }

                    FilledTonalButton(
                        onClick = {
                            tempPhotoUriString = null
                            showReturnConditionSheet = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Marcar como Devuelto (Sin Foto)")
                    }

                    OutlinedButton(
                        onClick = { 
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Capturar Foto y Marcar como Devuelto")
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

    if (showReturnConditionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReturnConditionSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "¿En qué condición fue devuelto?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                ReturnConditionButton(
                    icon = Icons.Default.Star,
                    label = "EXCELENTE",
                    description = "El artículo se devolvió en perfectas condiciones",
                    backgroundColor = Color(0xFF4CAF50),
                    onClick = {
                        viewModel.markAsReturnedWithCondition(tempPhotoUriString, "EXCELENTE")
                        showReturnConditionSheet = false
                        onNavigateBack()
                    }
                )

                ReturnConditionButton(
                    icon = Icons.Default.ThumbUp,
                    label = "BUENO",
                    description = "El artículo está en buen estado",
                    backgroundColor = Color(0xFF2196F3),
                    onClick = {
                        viewModel.markAsReturnedWithCondition(tempPhotoUriString, "BUENO")
                        showReturnConditionSheet = false
                        onNavigateBack()
                    }
                )

                ReturnConditionButton(
                    icon = Icons.Default.Warning,
                    label = "MALO",
                    description = "El artículo fue devuelto con daños",
                    backgroundColor = Color(0xFFFFA500),
                    onClick = {
                        viewModel.markAsReturnedWithCondition(tempPhotoUriString, "MALO")
                        showReturnConditionSheet = false
                        onNavigateBack()
                    }
                )

                ReturnConditionButton(
                    icon = Icons.Default.Cancel,
                    label = "NUNCA DEVUELTO",
                    description = "El artículo nunca fue devuelto",
                    backgroundColor = Color(0xFFD32F2F),
                    onClick = {
                        viewModel.markAsReturnedWithCondition(tempPhotoUriString, "NUNCA_DEVUELTO")
                        showReturnConditionSheet = false
                        onNavigateBack()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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

@Composable
fun ReturnConditionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, color = Color.White)
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
    }
}
