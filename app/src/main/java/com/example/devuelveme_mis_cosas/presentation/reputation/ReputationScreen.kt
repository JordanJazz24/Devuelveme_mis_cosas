package com.example.devuelveme_mis_cosas.presentation.reputation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
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
import com.example.devuelveme_mis_cosas.data.local.ContactReputation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReputationScreen(viewModel: ReputationViewModel = hiltViewModel()) {
    val reputations by viewModel.reputations.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reputación de Contactos") }
            )
        }
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
                    ReputationCard(reputation)
                }
            }
        }
    }
}

@Composable
private fun ReputationCard(reputation: ContactReputation) {
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

                // Badge de estado coloreado (Parte B)
                val (badgeText, badgeColor) = when {
                    reputation.neverReturned > 0 -> "PÉSIMO" to Color(0xFFD32F2F)
                    reputation.returnedDamaged > 0 -> "MALO" to Color(0xFFF57C00)
                    reputation.returnedLate > 0 -> "BUENO" to Color(0xFF1976D2)
                    else -> "EXCELENTE" to Color(0xFF388E3C)
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Score Visual con Estrellas
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
                        modifier = Modifier.size(18.dp),
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

            // Resumen de estadísticas (Formato solicitado Parte B)
            val devueltos = reputation.returnedOnTime + reputation.returnedLate + reputation.returnedDamaged
            Text(
                text = "${reputation.totalLoans} préstamos · $devueltos devueltos · ${reputation.neverReturned} nunca devueltos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
