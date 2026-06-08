package com.example.devuelveme_mis_cosas.presentation.loan_list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.ui.theme.Amber
import com.example.devuelveme_mis_cosas.ui.theme.Emerald
import com.example.devuelveme_mis_cosas.ui.theme.Rose
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanListScreen(
    onNavigateToNewLoan: () -> Unit,
    onNavigateToDetail: (UUID) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: LoanListViewModel = hiltViewModel()
) {
    val loans by viewModel.activeLoans.collectAsState()

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Main Header Row (Compacta)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hola 👋",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tus Préstamos",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Configuración", modifier = Modifier.size(20.dp))
                        }
                    }

                    // Quick Summary Chips (Compactos)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryChip(
                            label = "${loans.size} Activos",
                            color = Emerald
                        )
                        
                        val overdueCount = loans.count { it.fechaDevolucion.time < System.currentTimeMillis() }
                        if (overdueCount > 0) {
                            SummaryChip(
                                label = "$overdueCount Vencidos",
                                color = Rose
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewLoan,
                containerColor = Emerald,
                contentColor = Color.White,
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No hay préstamos activos",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
fun SummaryChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
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
    val isOverdue = urgency.label == "Vencido"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isOverdue) Rose.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min), // Para que el acento vertical ocupe todo el alto
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Overdue accent
            if (isOverdue) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(Rose)
                )
            }

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image or Placeholder
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (loan.photoLoanUri != null) {
                        AsyncImage(
                            model = loan.photoLoanUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loan.nombreObjeto,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = loan.contactoNombre,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Pill
                    Surface(
                        color = urgency.bgColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                urgency.icon,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = urgency.color
                            )
                            Text(
                                text = "${urgency.label}: ${dateFormatter.format(loan.fechaDevolucion)}",
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
}

data class UrgencyStatus(
    val color: Color,
    val bgColor: Color,
    val label: String,
    val icon: ImageVector
)

private fun getUrgencyStatus(returnDate: Date): UrgencyStatus {
    val currentTime = System.currentTimeMillis()
    val diffInMillis = returnDate.time - currentTime
    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

    return when {
        diffInMillis < 0 -> UrgencyStatus(
            color = Rose,
            bgColor = Rose.copy(alpha = 0.1f),
            label = "Vencido",
            icon = Icons.Default.ErrorOutline
        )
        diffInDays <= 7 -> UrgencyStatus(
            color = Amber,
            bgColor = Amber.copy(alpha = 0.1f),
            label = "Próximo",
            icon = Icons.Default.Schedule
        )
        else -> UrgencyStatus(
            color = Emerald,
            bgColor = Emerald.copy(alpha = 0.1f),
            label = "A tiempo",
            icon = Icons.Default.CheckCircle
        )
    }
}
