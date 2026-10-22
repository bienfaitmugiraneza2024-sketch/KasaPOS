package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.StockMovementEntity
import com.example.ui.state.KasaViewModel
import com.example.ui.theme.AlertWarning
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StockMovementHistoryDialog(
    viewModel: KasaViewModel,
    targetProductId: String? = null,
    targetProductName: String? = null,
    onDismiss: () -> Unit
) {
    val allMovements by viewModel.recentStockMovements.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "ENTRY", "SALE", "LOSS", "ADJUSTMENT"

    val filteredMovements = remember(allMovements, targetProductId, selectedFilter) {
        allMovements.filter { mov ->
            val matchProduct = targetProductId == null || mov.productId == targetProductId
            val matchFilter = when (selectedFilter) {
                "ENTRY" -> mov.movementType == "ENTRY" || mov.movementType == "INITIAL"
                "SALE" -> mov.movementType == "SALE"
                "LOSS" -> mov.movementType == "LOSS"
                "ADJUSTMENT" -> mov.movementType == "ADJUSTMENT"
                else -> true
            }
            matchProduct && matchFilter
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (targetProductName != null) "Mouvements : $targetProductName" else "Journal des Mouvements de Stock",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Audit trail consigné dans la base Room SQLite",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Tous", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "ENTRY",
                        onClick = { selectedFilter = "ENTRY" },
                        label = { Text("Entrées (+)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "SALE",
                        onClick = { selectedFilter = "SALE" },
                        label = { Text("Ventes (-)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "LOSS",
                        onClick = { selectedFilter = "LOSS" },
                        label = { Text("Pertes", fontSize = 11.sp) }
                    )
                }

                if (filteredMovements.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aucun mouvement de stock enregistré",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredMovements, key = { it.id }) { movement ->
                            StockMovementRow(movement = movement)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_movement_history_button")
            ) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun StockMovementRow(movement: StockMovementEntity) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateStr = remember(movement.timestamp) { sdf.format(Date(movement.timestamp)) }

    val (badgeBg, badgeFg, icon, typeLabel) = when (movement.movementType) {
        "ENTRY", "INITIAL" -> Quadruple(
            ProfitGreen.copy(alpha = 0.15f),
            ProfitGreen,
            Icons.Default.AddCircle,
            if (movement.movementType == "INITIAL") "Stock Initial" else "Réapprovisionnement"
        )
        "SALE" -> Quadruple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.ShoppingBag,
            "Vente Caisse"
        )
        "LOSS" -> Quadruple(
            LossRed.copy(alpha = 0.15f),
            LossRed,
            Icons.Default.RemoveCircle,
            "Perte / Dégradation"
        )
        "TRANSFER" -> Quadruple(
            Color(0xFF0288D1).copy(alpha = 0.15f),
            Color(0xFF0288D1),
            Icons.Default.SwapHoriz,
            "Transfert Stock"
        )
        else -> Quadruple(
            AlertWarning.copy(alpha = 0.15f),
            AlertWarning,
            Icons.Default.Tune,
            "Ajustement Inventaire"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeFg,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = movement.productName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Text(
                        text = if (movement.quantityChange >= 0) "+${movement.quantityChange.toInt()}" else "${movement.quantityChange.toInt()}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (movement.quantityChange >= 0) ProfitGreen else LossRed
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$typeLabel • $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${movement.previousStock.toInt()} ➔ ${movement.newStock.toInt()}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (movement.reason.isNotBlank()) {
                    Text(
                        text = "Motif : ${movement.reason} (${movement.operatorName})",
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
