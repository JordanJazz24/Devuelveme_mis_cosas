package com.example.devuelveme_mis_cosas.presentation.reputation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.devuelveme_mis_cosas.data.local.ContactReputation
import com.example.devuelveme_mis_cosas.presentation.components.AnimatedSuccessDialog
import com.example.devuelveme_mis_cosas.presentation.components.ConfirmationDialog

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReputationScreen(viewModel: ReputationViewModel = hiltViewModel()) {
    val reputations by viewModel.reputations.collectAsState()
    var reputationToDelete by remember { mutableStateOf<ContactReputation?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var deletedContactName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reputación de Contactos") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (reputations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Sin registros de reputación aún", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reputations) { reputation ->
                    ReputationCard(
                        reputation = reputation,
                        onDeleteClick = { reputationToDelete = reputation }
                    )
                }
            }
        }
    }

    if (reputationToDelete != null) {
        ConfirmationDialog(
            title = "Eliminar Reputación",
            message = "¿Estás seguro de eliminar a este contacto de tu historial? Sus préstamos no se borrarán, pero su reputación sí.",
            confirmText = "Eliminar",
            onConfirm = {
                val name = reputationToDelete?.contactName ?: ""
                viewModel.deleteReputation(reputationToDelete!!)
                deletedContactName = name
                reputationToDelete = null
                showSuccessDialog = true
            },
            onDismiss = { reputationToDelete = null }
        )
    }

    if (showSuccessDialog) {
        AnimatedSuccessDialog(
            title = "¡Eliminado!",
            message = "La reputación de $deletedContactName ha sido borrada.",
            onDismiss = { showSuccessDialog = false }
        )
    }
}

// PROBLEMA 3: Funciones puras para el badge según el score real
fun getBadgeLabel(score: Float): String = when {
    score >= 4.5f -> "EXCELENTE"
    score >= 3.0f -> "BUENO"
    score >= 1.5f -> "MALO"
    else          -> "PÉSIMO"
}

fun getBadgeColor(score: Float): Color = when {
    score >= 4.5f -> Color(0xFF27AE60)  // verde
    score >= 3.0f -> Color(0xFF2980B9)  // azul
    score >= 1.5f -> Color(0xFFE67E22)  // naranja
    else          -> Color(0xFFC0392B)  // rojo
}

@Composable
private fun ReputationCard(
    reputation: ContactReputation,
    onDeleteClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (reputation.contactPhotoUri != null) {
                    AsyncImage(
                        model = reputation.contactPhotoUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(reputation.contactName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(reputation.contactPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Badge calculado dinámicamente por función pura
                val badgeText = getBadgeLabel(reputation.reputationScore)
                val badgeColor = getBadgeColor(reputation.reputationScore)

                Surface(
                    color = badgeColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Black
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar Reputación",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Score Visual
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(5) { index ->
                    val starIndex = index + 1
                    val icon = when {
                        reputation.reputationScore >= starIndex -> Icons.Default.Star
                        reputation.reputationScore >= starIndex - 0.5f -> Icons.AutoMirrored.Filled.StarHalf
                        else -> Icons.Default.StarOutline
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFFFB300)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "%.1f".format(reputation.reputationScore),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Resumen de estadísticas (Formato exacto solicitado)
            val devueltos = reputation.returnedOnTime + reputation.returnedLate
            Text(
                text = "${reputation.totalLoans} préstamos · $devueltos devueltos · ${reputation.neverReturned} nunca devueltos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
