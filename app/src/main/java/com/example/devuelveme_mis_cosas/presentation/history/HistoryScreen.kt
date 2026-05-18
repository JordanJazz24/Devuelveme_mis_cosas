package com.example.devuelveme_mis_cosas.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDetail: (UUID) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val loans by viewModel.returnedLoans.collectAsState()
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Préstamos") }
            )
        }
    ) { padding ->
        if (loans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay préstamos devueltos aún")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(loans) { loan ->
                    HistoryItem(
                        loan = loan,
                        onClick = { onNavigateToDetail(loan.id) },
                        dateFormatter = dateFormatter
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    loan: LoanEntity,
    onClick: () -> Unit,
    dateFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Foto inicial
                Box(modifier = Modifier.weight(1f)) {
                    LoanThumbnail(uri = loan.photoLoanUri, label = "Inicial")
                }
                // Foto devolución
                Box(modifier = Modifier.weight(1f)) {
                    LoanThumbnail(uri = loan.photoReturnUri, label = "Devolución")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                Text(
                    text = loan.nombreObjeto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "De: ${loan.contactoNombre}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                val returnDate = loan.fechaDevolucionReal ?: loan.fechaDevolucion
                Text(
                    text = "Devuelto el: ${dateFormatter.format(returnDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LoanThumbnail(uri: String?, label: String) {
    if (uri != null) {
        AsyncImage(
            model = uri,
            contentDescription = label,
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
