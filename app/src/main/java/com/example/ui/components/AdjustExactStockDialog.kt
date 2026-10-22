package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.ui.state.KasaViewModel
import com.example.ui.theme.AlertWarning
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen

@Composable
fun AdjustExactStockDialog(
    product: ProductEntity,
    viewModel: KasaViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var countedStockText by remember { mutableStateOf(product.stockQuantity.toInt().toString()) }
    var selectedReasonPreset by remember { mutableStateOf("Comptage physique périodique") }
    var customReason by remember { mutableStateOf("") }

    val countedStock = countedStockText.toDoubleOrNull() ?: product.stockQuantity
    val difference = countedStock - product.stockQuantity

    val presets = listOf(
        "Comptage physique périodique",
        "Correction d'erreur de saisie",
        "Retour client non consigné",
        "Recomptage après inventaire"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Ajustement d'Inventaire",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Current Stock vs Counted Stock Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Stock Théorique (Système)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${product.stockQuantity.toInt()} ${product.unit}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Écart constaté", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val diffColor = when {
                                difference > 0 -> ProfitGreen
                                difference < 0 -> LossRed
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            val diffPrefix = if (difference > 0) "+" else ""
                            Text(
                                text = "$diffPrefix${difference.toInt()} ${product.unit}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = diffColor
                                )
                            )
                        }
                    }
                }

                // Counted stock input
                OutlinedTextField(
                    value = countedStockText,
                    onValueChange = { countedStockText = it },
                    label = { Text("Stock Réel Compté (${product.unit}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_counted_stock")
                )

                Text(
                    text = "Motif de l'ajustement :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = selectedReasonPreset == preset,
                            onClick = { selectedReasonPreset = preset },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = customReason,
                    onValueChange = { customReason = it },
                    label = { Text("Observation complémentaire (optionnel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (customReason.isNotBlank()) "$selectedReasonPreset - $customReason" else selectedReasonPreset
                    viewModel.adjustExactStock(
                        productId = product.id,
                        exactStock = countedStock,
                        reason = finalReason,
                        onSuccess = {
                            onSuccess()
                            onDismiss()
                        }
                    )
                },
                modifier = Modifier.testTag("confirm_adjust_stock_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Valider le nouveau stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
