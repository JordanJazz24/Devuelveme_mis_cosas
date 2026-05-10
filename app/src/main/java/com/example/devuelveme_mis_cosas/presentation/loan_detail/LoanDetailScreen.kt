package com.example.devuelveme_mis_cosas.presentation.loan_detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: LoanDetailViewModel = hiltViewModel()
) {
    val loan by viewModel.loan.collectAsState()
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Préstamo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                if (entity.photoLoanUri != null) {
                    AsyncImage(
                        model = entity.photoLoanUri,
                        contentDescription = "Foto del objeto",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sin foto", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Text(
                    text = entity.nombreObjeto,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Divider()

                DetailItem(label = "Contacto", value = entity.contactoNombre)
                if (entity.contactoTelefono.isNotBlank()) {
                    DetailItem(label = "Teléfono", value = entity.contactoTelefono)
                }
                DetailItem(label = "Fecha de préstamo", value = dateFormatter.format(Date(entity.fechaPrestamo)))
                DetailItem(label = "Fecha de devolución", value = dateFormatter.format(Date(entity.fechaDevolucion)))

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { /* TODO: WhatsApp en Fase 3 */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Recordar por WhatsApp")
                }

                OutlinedButton(
                    onClick = { /* TODO: Marcar como devuelto en Fase 3 */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Marcar como Devuelto")
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
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
