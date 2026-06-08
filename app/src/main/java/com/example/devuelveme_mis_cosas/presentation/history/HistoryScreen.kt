package com.example.devuelveme_mis_cosas.presentation.history

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.devuelveme_mis_cosas.data.local.LoanEntity
import com.example.devuelveme_mis_cosas.presentation.components.AnimatedSuccessDialog
import com.example.devuelveme_mis_cosas.presentation.components.ConfirmationDialog
import com.example.devuelveme_mis_cosas.ui.theme.Amber
import com.example.devuelveme_mis_cosas.ui.theme.Emerald
import com.example.devuelveme_mis_cosas.ui.theme.Rose
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onNavigateToDetail: (UUID) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val groupedLoans by viewModel.groupedLoans.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var loanToDelete by remember { mutableStateOf<LoanEntity?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Historial",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    IconButton(
                        onClick = { showDeleteAllDialog = true },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Vaciar historial", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Search Pill
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().height(78.dp),
                    placeholder = { Text("Buscar préstamo o contacto...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(percent = 50),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { padding ->
        if (groupedLoans.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (searchQuery.isEmpty()) "No hay préstamos devueltos aún" else "No se encontraron resultados",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedLoans.forEach { (month, loans) ->
                    stickyHeader {
                        val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawLine(
                                        color = borderColor,
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                },
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = month.uppercase(),
                                modifier = Modifier.padding(vertical = 12.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    items(loans, key = { it.id }) { loan ->
                        SwipeToDeleteWrapper(
                            onDelete = { loanToDelete = loan }
                        ) {
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
    }

    if (showDeleteAllDialog) {
        ConfirmationDialog(
            title = "¿Vaciar historial?",
            message = "¿Estás seguro de que deseas borrar todo el historial? Esta acción no se puede deshacer.",
            confirmText = "Vaciar",
            onConfirm = {
                viewModel.deleteAllHistory()
                showDeleteAllDialog = false
                successMessage = "Historial vaciado correctamente"
                showSuccessDialog = true
            },
            onDismiss = { showDeleteAllDialog = false }
        )
    }

    if (loanToDelete != null) {
        ConfirmationDialog(
            title = "¿Eliminar registro?",
            message = "¿Estás seguro de que deseas eliminar este registro del historial?",
            confirmText = "Eliminar",
            onConfirm = {
                viewModel.deleteLoan(loanToDelete!!)
                successMessage = "Registro eliminado"
                loanToDelete = null
                showSuccessDialog = true
            },
            onDismiss = { loanToDelete = null }
        )
    }

    if (showSuccessDialog) {
        AnimatedSuccessDialog(
            title = "¡Hecho!",
            message = successMessage,
            onDismiss = { showSuccessDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteWrapper(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                false // No lo borramos del UI inmediatamente, esperamos la confirmación
            } else false
        }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart || 
                dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                MaterialTheme.colorScheme.errorContainer
            } else Color.Transparent

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = {
            Box(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    )
}

@Composable
fun HistoryItem(
    loan: LoanEntity,
    onClick: () -> Unit,
    dateFormatter: SimpleDateFormat
) {
    val conditionColor = getConditionColor(loan.returnCondition)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loan.nombreObjeto,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "De: ${loan.contactoNombre}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    color = conditionColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = (loan.returnCondition ?: "CERRADO").replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = conditionColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    HistoryThumbnail(uri = loan.photoLoanUri, label = "INICIAL")
                }
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                
                Box(modifier = Modifier.weight(1f)) {
                    HistoryThumbnail(uri = loan.photoReturnUri, label = "DEVUELTO")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val returnDate = loan.fechaDevolucionReal ?: loan.fechaDevolucion
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.EventAvailable, 
                    null, 
                    modifier = Modifier.size(14.dp),
                    tint = Emerald
                )
                Text(
                    text = "Devuelto el ${dateFormatter.format(returnDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HistoryThumbnail(uri: String?, label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ImageNotSupported,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
        
        // Label overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    RoundedCornerShape(topEnd = 8.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun getConditionColor(condition: String?): Color {
    return when (condition) {
        "EXCELENTE" -> Emerald
        "BUENO" -> Emerald.copy(alpha = 0.7f)
        "MALO" -> Amber
        "NUNCA_DEVUELTO" -> Rose
        else -> MaterialTheme.colorScheme.secondary
    }
}
