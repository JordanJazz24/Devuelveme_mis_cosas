package com.example.devuelveme_mis_cosas.presentation.loan_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanListScreen(
    onNavigateToNewLoan: () -> Unit,
    onNavigateToDetail: (UUID) -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: LoanListViewModel = hiltViewModel()
) {
    val loans by viewModel.activeLoans.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Préstamos Activos") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "Ver Historial")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNewLoan) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Préstamo")
            }
        }
    ) { padding ->
        if (loans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay préstamos activos")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(loans) { loan ->
                    LoanItem(
                        loan = loan,
                        onClick = { onNavigateToDetail(loan.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun LoanItem(
    loan: LoanEntity,
    onClick: () -> Unit
) {
    val urgencyColor = getUrgencyColor(loan.fechaDevolucion)
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = urgencyColor.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, urgencyColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loan.photoLoanUri != null) {
                AsyncImage(
                    model = loan.photoLoanUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = loan.nombreObjeto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A: ${loan.contactoNombre}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Límite: ${dateFormatter.format(loan.fechaDevolucion)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = urgencyColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getUrgencyColor(returnDate: Date): Color {
    val currentTime = System.currentTimeMillis()
    val diffInMillis = returnDate.time - currentTime
    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

    return when {
        diffInMillis < 0 -> Color.Red // Vencido
        diffInDays <= 7 -> Color(0xFFFFC107) // Amarillo (Amber)
        else -> Color(0xFF4CAF50) // Verde
    }
}
