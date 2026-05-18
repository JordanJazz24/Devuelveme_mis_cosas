package com.example.devuelveme_mis_cosas.presentation.loan_list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.ui.theme.Amber
import com.example.devuelveme_mis_cosas.ui.theme.Emerald
import com.example.devuelveme_mis_cosas.ui.theme.PrimaryViolet
import com.example.devuelveme_mis_cosas.ui.theme.PrimaryVioletDark
import com.example.devuelveme_mis_cosas.ui.theme.Rose
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
            LargeTopAppBar(
                title = { 
                    Text(
                        "Préstamos Activos",
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historial",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // FAB Corregido: Sin clipping manual ni fondos transparentes conflictivos
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewLoan,
                containerColor = MaterialTheme.colorScheme.primary, // Color sólido para un look más limpio
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo Préstamo", fontWeight = FontWeight.Bold) }
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
                Text(
                    "No hay préstamos activos",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Espaciado un poco más compacto
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
    val urgency = getUrgencyStatus(loan.fechaDevolucion)
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp), // Esquinas ligeramente menos extremas para mejor balance
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp), // Eliminamos height(IntrinsicSize.Min)
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loan.photoLoanUri != null) {
                AsyncImage(
                    model = loan.photoLoanUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(70.dp) // Tamaño ajustado
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = loan.nombreObjeto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A: ${loan.contactoNombre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Indicador de urgencia más discreto y elegante
                Surface(
                    color = urgency.color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = urgency.color
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Vence: ${dateFormatter.format(loan.fechaDevolucion)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = urgency.color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

data class UrgencyStatus(val color: Color, val label: String)

private fun getUrgencyStatus(returnDate: Date): UrgencyStatus {
    val currentTime = System.currentTimeMillis()
    val diffInMillis = returnDate.time - currentTime
    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

    return when {
        diffInMillis < 0 -> UrgencyStatus(Rose, "Vencido")
        diffInDays <= 7 -> UrgencyStatus(Amber, "Próximo")
        else -> UrgencyStatus(Emerald, "A tiempo")
    }
}
