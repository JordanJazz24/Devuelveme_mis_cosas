package com.example.devuelveme_mis_cosas.presentation.loan_detail

import android.Manifest
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import com.example.devuelveme_mis_cosas.presentation.components.PermissionDialog
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
    var showCameraRationale by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUriString != null) {
            viewModel.markAsReturned(tempPhotoUriString)
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
            showCameraRationale = true
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    LaunchedEffect(uiState.reminderMessage) {
        uiState.reminderMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearReminderMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        loan?.let { entity ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                // Header con imagen inmersiva
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    if (entity.photoLoanUri != null) {
                        AsyncImage(
                            model = entity.photoLoanUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Gradiente inferior para legibilidad del título
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )

                    // Top Controls (Botón volver y borrar)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.3f)
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                        }

                        IconButton(
                            onClick = { showDeleteDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.3f)
                            )
                        ) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = Color.White)
                        }
                    }

                    // Título y Estado sobre el gradiente inferior
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = entity.nombreObjeto,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SuggestionChip(
                            onClick = { },
                            label = { Text(entity.estado.name) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (entity.estado == LoanStatus.ACTIVO) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                else 
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                labelColor = Color.White
                            ),
                            border = null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Información Detallada en Grid/Cards
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            InfoRow(Icons.Default.Person, "Persona", entity.contactoNombre)
                            if (entity.contactoTelefono.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                InfoRow(Icons.Default.Phone, "Contacto", entity.contactoTelefono)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            InfoRow(Icons.Default.Category, "Categoría", entity.categoria.name)
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            InfoRow(Icons.Default.CalendarToday, "Fecha Préstamo", dateFormatter.format(entity.fechaPrestamo))
                            Spacer(modifier = Modifier.height(16.dp))
                            InfoRow(
                                Icons.Default.CalendarToday, 
                                if (entity.estado == LoanStatus.ACTIVO) "Fecha Límite" else "Fecha Devolución", 
                                dateFormatter.format(entity.fechaDevolucion)
                            )
                        }
                    }

                    // Foto de Devolución (Si existe)
                    if (entity.estado == LoanStatus.DEVUELTO && entity.photoReturnUri != null) {
                        Text(
                            "Evidencia de Devolución",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        AsyncImage(
                            model = entity.photoReturnUri,
                            contentDescription = "Foto devolución",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Acciones
                    if (entity.estado == LoanStatus.ACTIVO) {
                        Button(
                            onClick = { viewModel.sendReminder() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Recordar por WhatsApp", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.markAsReturned(null) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Devuelto")
                            }
                            FilledTonalButton(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.weight(1.5f).height(50.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cerrar con Foto")
                            }
                        }
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

    // Dialogs
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar registro?") },
            text = { Text("Esta acción borrará permanentemente la información del préstamo.") },
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

    if (showCameraRationale) {
        PermissionDialog(
            permissionName = "Cámara",
            rationale = "Necesitamos la cámara para guardar una foto del estado en que te devuelven el objeto.",
            onDismiss = { showCameraRationale = false },
            onConfirm = {
                showCameraRationale = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
