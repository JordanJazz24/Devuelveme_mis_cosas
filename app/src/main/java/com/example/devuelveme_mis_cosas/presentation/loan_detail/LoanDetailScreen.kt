package com.example.devuelveme_mis_cosas.presentation.loan_detail

import android.Manifest
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.devuelveme_mis_cosas.data.local.LoanCategory
import com.example.devuelveme_mis_cosas.data.local.LoanStatus
import com.example.devuelveme_mis_cosas.presentation.components.*
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
    val payments by viewModel.payments.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val snackbarHostState = remember { SnackbarHostState() }
    
    var tempPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var showLostDialog by remember { mutableStateOf(false) }
    var showCameraRationale by remember { mutableStateOf(false) }
    var showReturnConditionSheet by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showSettleDebtDialog by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

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
            showCameraRationale = true
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            showSuccessDialog = true
        }
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
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { selectedImageUri = entity.photoLoanUri },
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
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val statusLabel = if (entity.estado == LoanStatus.DEVUELTO && entity.returnCondition == "NUNCA_DEVUELTO") 
                                "NUNCA DEVUELTO" 
                            else 
                                entity.estado.name

                            SuggestionChip(
                                onClick = { },
                                label = { Text(statusLabel) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (entity.estado == LoanStatus.ACTIVO) 
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    else if (entity.returnCondition == "NUNCA_DEVUELTO")
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                    labelColor = Color.White
                                ),
                                border = null,
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (entity.estado == LoanStatus.DEVUELTO && entity.returnCondition != null && entity.returnCondition != "NUNCA_DEVUELTO") {
                                val conditionColor = when(entity.returnCondition) {
                                    "EXCELENTE" -> Color(0xFF27AE60)
                                    "BUENO" -> Color(0xFF2980B9)
                                    "MALO" -> Color(0xFFE67E22)
                                    "NUNCA_DEVUELTO" -> Color(0xFFC0392B)
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(entity.returnCondition) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = conditionColor.copy(alpha = 0.8f),
                                        labelColor = Color.White
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
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
                            
                            val isEditable = entity.estado == LoanStatus.ACTIVO
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(if (isEditable) Modifier.clickable { showDueDatePicker = true } else Modifier)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    InfoRow(
                                        Icons.Default.CalendarMonth, 
                                        "Límite Acordado", 
                                        dateFormatter.format(entity.fechaDevolucion)
                                    )
                                    if (isEditable) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Editar fecha",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (entity.fechaDevolucionReal != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                InfoRow(
                                    Icons.Default.EventAvailable, 
                                    "Devuelto el", 
                                    dateFormatter.format(entity.fechaDevolucionReal)
                                )
                            }
                        }
                    }

                    if (entity.reminderCount > 0) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                InfoRow(
                                    Icons.Default.NotificationsActive,
                                    "Recordatorios enviados",
                                    entity.reminderCount.toString()
                                )
                                entity.lastReminderTimestamp?.let {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Último: ${dateFormatter.format(it)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(start = 36.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (entity.categoria == LoanCategory.DINERO) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Resumen de Deuda",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        color = if ((entity.remainingAmount ?: 0.0) <= 0.01) 
                                            Color(0xFF2E7D32).copy(alpha = 0.1f) 
                                        else 
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if ((entity.remainingAmount ?: 0.0) <= 0.01) "SALDADO" else "PENDIENTE",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if ((entity.remainingAmount ?: 0.0) <= 0.01) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DebtItem("Original", "$${entity.loanAmount ?: 0.0}", MaterialTheme.colorScheme.onSurface)
                                    DebtItem("Abonado", "$${"%.2f".format((entity.loanAmount ?: 0.0) - (entity.remainingAmount ?: 0.0))}", Color(0xFF2E7D32))
                                    DebtItem("Pendiente", "$${entity.remainingAmount ?: 0.0}", MaterialTheme.colorScheme.error)
                                }
                                
                                if (entity.estado == LoanStatus.ACTIVO) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { showPaymentDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(12.dp)
                                    ) {
                                        Icon(Icons.Default.AddCard, null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Registrar Abono", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (payments.isNotEmpty()) {
                            Text("Historial de Abonos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            payments.forEach { payment ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    ListItem(
                                        headlineContent = { Text("$${payment.amount}", fontWeight = FontWeight.Bold) },
                                        supportingContent = { 
                                            Column {
                                                Text(dateFormatter.format(payment.paymentDate))
                                                payment.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                            }
                                        },
                                        leadingContent = { Icon(Icons.Default.Payment, null, tint = Color(0xFF27AE60)) }
                                    )
                                }
                            }
                        }
                    }

                    if (!entity.notes.isNullOrBlank()) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Notas",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = entity.notes,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

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
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { selectedImageUri = entity.photoReturnUri },
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
                                onClick = { showReturnDialog = true },
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

                        TextButton(
                            onClick = { showLostDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dar por Perdido / Nunca Devuelto")
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

    if (showReturnDialog) {
        ConfirmationDialog(
            title = "Marcar como devuelto",
            message = "¿Estás seguro de que quieres marcar este artículo como devuelto? Esta acción no se puede deshacer.",
            onConfirm = {
                tempPhotoUriString = null
                if (loan?.categoria == LoanCategory.DINERO && (loan?.remainingAmount ?: 0.0) > 0.01) {
                    showReturnDialog = false
                    showSettleDebtDialog = true
                } else {
                    if (loan?.categoria == LoanCategory.DINERO) {
                        successMessage = "¡Dinero devuelto con éxito!"
                        viewModel.markAsReturnedWithCondition(null, "BUENO")
                    } else {
                        showReturnConditionSheet = true
                    }
                }
            },
            onDismiss = { showReturnDialog = false }
        )
    }

    if (showSettleDebtDialog) {
        AlertDialog(
            onDismissRequest = { showSettleDebtDialog = false },
            title = { Text("¿Saldar deuda?") },
            text = { Text("El préstamo aún tiene un saldo pendiente de $${loan?.remainingAmount}. ¿Quieres marcarlo como saldado (pagado por completo) al cerrar el préstamo?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markAsReturnedWithCondition(null, "BUENO", settleDebt = true)
                    successMessage = "Préstamo saldado y cerrado con éxito."
                    showSettleDebtDialog = false
                }) { Text("Sí, saldar todo") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.markAsReturnedWithCondition(null, "BUENO", settleDebt = false)
                    successMessage = "Préstamo cerrado. El saldo queda como pendiente."
                    showSettleDebtDialog = false
                }) { Text("No, cerrar con deuda") }
            }
        )
    }

    if (showLostDialog) {
        ConfirmationDialog(
            title = "¿Dar por perdido?",
            message = "Esto afectará negativamente la reputación del contacto. ¿Estás seguro?",
            confirmText = "Confirmar",
            onConfirm = {
                successMessage = "Préstamo cerrado como no devuelto."
                viewModel.markAsReturnedWithCondition(photoReturnUri = null, condition = "NUNCA_DEVUELTO", settleDebt = false)
            },
            onDismiss = { showLostDialog = false }
        )
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "¿Eliminar registro?",
            message = "Esta acción borrará permanentemente la información del préstamo.",
            confirmText = "Eliminar",
            onConfirm = {
                successMessage = "Préstamo eliminado correctamente"
                viewModel.deleteLoan()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showSuccessDialog) {
        AnimatedSuccessDialog(
            title = "¡Hecho!",
            message = successMessage,
            onDismiss = {
                showSuccessDialog = false
                onNavigateBack()
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
                        successMessage = "¡Artículo devuelto en excelentes condiciones!"
                        viewModel.markAsReturnedWithCondition(tempPhotoUriString, "EXCELENTE", settleDebt = false)
                        showReturnConditionSheet = false
                    }
                )

                ReturnConditionButton(
                    icon = Icons.Default.ThumbUp,
                    label = "BUENO",
                    description = "El artículo está en buen estado",
                    backgroundColor = Color(0xFF2196F3),
                    onClick = {
                        successMessage = "¡Artículo devuelto correctamente!"
                        viewModel.markAsReturnedWithCondition(tempPhotoUriString, "BUENO", settleDebt = false)
                        showReturnConditionSheet = false
                    }
                )

                ReturnConditionButton(
                    icon = Icons.Default.Warning,
                    label = "MALO",
                    description = "El artículo fue devuelto con daños",
                    backgroundColor = Color(0xFFFFA500),
                    onClick = {
                        successMessage = "Registro actualizado. El artículo tiene daños."
                        viewModel.markAsReturnedWithCondition(tempPhotoUriString, "MALO", settleDebt = false)
                        showReturnConditionSheet = false
                    }
                )



                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showPaymentDialog) {
        var amount by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Registrar Abono") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Monto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Nota (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.registerPayment(amount, note)
                        showPaymentDialog = false
                    }
                ) { Text("Registrar") }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showDueDatePicker && loan != null) {
        DatePickerModal(
            initialDate = viewModel.getUtcMillis(loan!!.fechaDevolucion),
            onDateSelected = { viewModel.updateReturnDate(it) },
            onDismiss = { showDueDatePicker = false }
        )
    }

    selectedImageUri?.let { uri ->
        FullScreenImageDialog(
            imageUri = uri,
            onDismiss = { selectedImageUri = null }
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

@Composable
fun DebtItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
fun ReturnConditionButton(
    icon: ImageVector,
    label: String,
    description: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
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
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
        }
    }
}
