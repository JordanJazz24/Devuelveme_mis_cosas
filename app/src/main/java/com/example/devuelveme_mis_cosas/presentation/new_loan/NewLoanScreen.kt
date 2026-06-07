package com.example.devuelveme_mis_cosas.presentation.new_loan

import android.Manifest
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.devuelveme_mis_cosas.data.local.LoanCategory
import com.example.devuelveme_mis_cosas.presentation.components.AnimatedSuccessDialog
import com.example.devuelveme_mis_cosas.presentation.components.FullScreenImageDialog
import com.example.devuelveme_mis_cosas.presentation.components.PermissionDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLoanScreen(
    onNavigateBack: () -> Unit,
    viewModel: NewLoanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val snackbarHostState = remember { SnackbarHostState() }

    var showLoanDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    
    var tempPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showFullImage by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showPhotoSourcePicker by remember { mutableStateOf(false) }

    var showCameraRationale by remember { mutableStateOf(false) }
    var showContactsRationale by remember { mutableStateOf(false) }

    // Launchers (Logic preserved)
    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) viewModel.onContactPicked(uri)
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            showContactsRationale = true
        }
    }

    val createContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            if (result.data?.data != null) {
                viewModel.onContactPicked(result.data!!.data!!)
            } else {
                viewModel.onContactCreatedFallback()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUriString != null) {
            viewModel.onPhotoSelected(Uri.parse(tempPhotoUriString))
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onPhotoSelected(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "loan_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempPhotoUriString = uri.toString()
            cameraLauncher.launch(uri)
        } else {
            showCameraRationale = true
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) showSuccessDialog = true
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Préstamo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Step 2: Modern Inputs
            OutlinedTextField(
                value = uiState.nombreObjeto,
                onValueChange = viewModel::onNombreObjetoChange,
                label = { Text("¿Qué has prestado?") },
                placeholder = { Text("Ej. Taladro, Libro, Dinero...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                isError = uiState.nombreObjeto.isBlank() && uiState.errorMessage != null,
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notas adicionales") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                minLines = 3
            )

            // Category Selector
            ExposedDropdownMenuBox(
                expanded = showCategoryMenu,
                onExpandedChange = { showCategoryMenu = !showCategoryMenu }
            ) {
                OutlinedTextField(
                    value = uiState.categoria.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    LoanCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                viewModel.onCategoriaChange(category)
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }

            // Step 3: Redesigned Contact Card
            Text("Información del Contacto", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (uiState.contactoNombre.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text(uiState.contactoNombre, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { 
                            if (uiState.contactoTelefono.isNotBlank()) {
                                Text(uiState.contactoTelefono)
                            }
                        },
                        leadingContent = {
                            if (uiState.contactoPhotoUri != null) {
                                AsyncImage(
                                    model = uiState.contactoPhotoUri,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.onContactCleared() }) {
                                Icon(Icons.Default.Edit, contentDescription = "Cambiar contacto", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text("Asignar a un contacto", style = MaterialTheme.typography.bodyLarge)
                        
                        Button(
                            onClick = { contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Buscar en Agenda")
                        }
                        
                        TextButton(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                    type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
                                }
                                createContactLauncher.launch(intent)
                            }
                        ) {
                            Text("O crear nuevo contacto")
                        }
                    }
                }
            }

            // Step 4: Interactive Dates
            Text("Plazos", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DatePickerField(
                    label = "Prestado el",
                    date = uiState.fechaPrestamo,
                    onClick = { showLoanDatePicker = true },
                    modifier = Modifier.weight(1f),
                    dateFormatter = dateFormatter
                )
                DatePickerField(
                    label = "Devolución",
                    date = uiState.fechaDevolucion,
                    onClick = { showDueDatePicker = true },
                    modifier = Modifier.weight(1f),
                    dateFormatter = dateFormatter
                )
            }

            // Step 5: Photography Area
            Text("Evidencia Visual", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            if (uiState.photoUri != null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    AsyncImage(
                        model = uiState.photoUri,
                        contentDescription = "Foto del objeto",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showFullImage = true },
                        contentScale = ContentScale.Crop
                    )
                    FilledIconButton(
                        onClick = { viewModel.onPhotoSelected(null) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar foto")
                    }
                }
            } else {
                val strokeColor = MaterialTheme.colorScheme.outlineVariant
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .drawBehind {
                            drawRoundRect(
                                color = strokeColor,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                            )
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showPhotoSourcePicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Agregar foto de evidencia", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Step 6: CTA Save Button
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.saveLoan() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Guardar Préstamo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            // Bottom Spacing
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Modal for Photo Source
    if (showPhotoSourcePicker) {
        AlertDialog(
            onDismissRequest = { showPhotoSourcePicker = false },
            title = { Text("Seleccionar origen") },
            text = { Text("Elige cómo quieres agregar la foto de evidencia.") },
            confirmButton = {
                TextButton(onClick = {
                    showPhotoSourcePicker = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cámara")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoSourcePicker = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Galería")
                }
            }
        )
    }

    // Pickers and Dialogs (Preserved Logic)
    if (showLoanDatePicker) {
        DatePickerModal(
            initialDate = viewModel.getUtcMillis(uiState.fechaPrestamo),
            onDateSelected = { viewModel.onFechaPrestamoChange(it) },
            onDismiss = { showLoanDatePicker = false }
        )
    }

    if (showDueDatePicker) {
        DatePickerModal(
            initialDate = viewModel.getUtcMillis(uiState.fechaDevolucion),
            onDateSelected = { viewModel.onFechaDevolucionChange(it) },
            onDismiss = { showDueDatePicker = false }
        )
    }

    if (showCameraRationale) {
        PermissionDialog(
            permissionName = "Cámara",
            rationale = "Necesitamos acceso a la cámara para que puedas tomar fotos de los objetos que prestas.",
            onDismiss = { showCameraRationale = false },
            onConfirm = {
                showCameraRationale = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }

    if (showContactsRationale) {
        PermissionDialog(
            permissionName = "Contactos",
            rationale = "El acceso a los contactos permite seleccionar rápidamente a quién le prestas algo.",
            onDismiss = { showContactsRationale = false },
            onConfirm = {
                showContactsRationale = false
                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        )
    }

    if (showFullImage && uiState.photoUri != null) {
        FullScreenImageDialog(
            imageUri = uiState.photoUri.toString(),
            onDismiss = { showFullImage = false }
        )
    }

    if (showSuccessDialog) {
        AnimatedSuccessDialog(
            title = "¡Éxito!",
            message = "El préstamo se ha guardado correctamente.",
            onDismiss = {
                showSuccessDialog = false
                onNavigateBack()
            }
        )
    }
}

@Composable
fun DatePickerField(label: String, date: Date, onClick: () -> Unit, modifier: Modifier, dateFormatter: SimpleDateFormat) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                Text(dateFormatter.format(date), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(initialDate: Long, onDateSelected: (Long) -> Unit, onDismiss: () -> Unit) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                onDismiss()
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) { DatePicker(state = datePickerState) }
}
