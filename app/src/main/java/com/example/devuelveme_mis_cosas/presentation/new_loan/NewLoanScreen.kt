package com.example.devuelveme_mis_cosas.presentation.new_loan

import android.Manifest
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.devuelveme_mis_cosas.data.local.LoanCategory
import com.example.devuelveme_mis_cosas.domain.model.Contact
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    var showLoanDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    
    var tempPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleContactPicker(true)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Permiso de contactos denegado. Puedes escribir el nombre manualmente.")
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
            scope.launch {
                snackbarHostState.showSnackbar("Permiso de cámara denegado. Puedes seleccionar una foto desde la galería.")
            }
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
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
                title = { Text("Nuevo Préstamo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveLoan() }) {
                        Text("GUARDAR", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = uiState.nombreObjeto,
                onValueChange = viewModel::onNombreObjetoChange,
                label = { Text("¿Qué has prestado? *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.nombreObjeto.isBlank() && uiState.errorMessage != null
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notas adicionales (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            ExposedDropdownMenuBox(
                expanded = showCategoryMenu,
                onExpandedChange = { showCategoryMenu = !showCategoryMenu }
            ) {
                TextField(
                    value = uiState.categoria.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
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

            // Contact Section - Conditional UI based on contactEnteredManually
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Contacto *", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!uiState.contactEnteredManually) {
                        // Mode: Contact Picker
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.contactoNombre.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (uiState.contactoPhotoUri != null) {
                                        AsyncImage(
                                            model = uiState.contactoPhotoUri,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column {
                                        Text(uiState.contactoNombre, fontWeight = FontWeight.Bold)
                                        if (uiState.contactoTelefono.isNotBlank()) {
                                            Text(uiState.contactoTelefono, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            } else {
                                Text("Selecciona un contacto", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = {
                                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }) {
                                Icon(Icons.Default.ContactPage, contentDescription = "Seleccionar contacto")
                            }
                        }
                    } else {
                        // Mode: Manual Entry
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(uiState.contactoNombre.ifBlank { "Nombre no ingresado" }, fontWeight = FontWeight.Bold)
                                if (uiState.contactoTelefono.isNotBlank()) {
                                    Text(uiState.contactoTelefono, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.contactoNombre,
                            onValueChange = viewModel::onContactoNombreChange,
                            label = { Text("Nombre del prestatario *") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.contactoNombre.isBlank() && uiState.errorMessage != null
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.contactoTelefono,
                            onValueChange = viewModel::onContactoTelefonoChange,
                            label = { Text("Número de teléfono *") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                            ),
                            isError = uiState.contactoTelefono.isBlank() && uiState.errorMessage != null
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!uiState.contactEnteredManually) {
                            TextButton(
                                onClick = { viewModel.onContactManualToggle(true) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ingresar manualmente", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                            }
                        } else {
                            TextButton(
                                onClick = { viewModel.onContactManualToggle(false) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Seleccionar de contactos", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                            }
                        }
                    }
                }
            }

            // Date Pickers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePickerField(
                    label = "Fecha Préstamo",
                    date = uiState.fechaPrestamo,
                    onClick = { showLoanDatePicker = true },
                    modifier = Modifier.weight(1f),
                    dateFormatter = dateFormatter
                )
                DatePickerField(
                    label = "Fecha Devolución",
                    date = uiState.fechaDevolucion,
                    onClick = { showDueDatePicker = true },
                    modifier = Modifier.weight(1f),
                    dateFormatter = dateFormatter
                )
            }

            // Photo Section
            if (uiState.photoUri != null) {
                AsyncImage(
                    model = uiState.photoUri,
                    contentDescription = "Foto del objeto",
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cámara")
                }
                OutlinedButton(
                    onClick = { 
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Galería")
                }
            }
        }
    }

    // Dialogs
    if (uiState.showContactPicker) {
        ContactPickerDialog(
            contacts = uiState.contacts,
            onContactSelected = viewModel::onContactSelected,
            onDismiss = { viewModel.toggleContactPicker(false) },
            searchQuery = uiState.contactSearchQuery,
            onSearchQueryChange = viewModel::onContactSearchQueryChange
        )
    }

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
}

@Composable
fun DatePickerField(label: String, date: Date, onClick: () -> Unit, modifier: Modifier, dateFormatter: SimpleDateFormat) {
    OutlinedCard(onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(dateFormatter.format(date), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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

@Composable
fun ContactPickerDialog(
    contacts: List<Contact>,
    onContactSelected: (Contact) -> Unit,
    onDismiss: () -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {}
) {
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            val q = searchQuery.lowercase().trim()
            contacts.filter {
                it.name.lowercase().contains(q) ||
                        it.phoneNumber?.contains(q) == true
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Seleccionar Contacto", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Busca un nombre o número de teléfono") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn {
                    items(filteredContacts) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onContactSelected(contact) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = contact.photoUri,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(contact.name, style = MaterialTheme.typography.bodyLarge)
                                contact.phoneNumber?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
