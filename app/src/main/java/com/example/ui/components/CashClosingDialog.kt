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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.state.KasaViewModel
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen

@Composable
fun CashClosingDialog(
    viewModel: KasaViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val storeObj = viewModel.selectedStore.collectAsState().value ?: viewModel.stores.collectAsState().value.firstOrNull()
    val todaySales = viewModel.getFilteredSales().filter { viewModel.isToday(it.createdAt) }

    val systemSalesUSD = todaySales.filter { it.currencyUsed == "USD" }.sumOf { it.totalUSD }
    val systemSalesFC = todaySales.filter { it.currencyUsed == "FC" }.sumOf { it.totalFC }

    var countedUSDInput by remember { mutableStateOf("") }
    var countedFCInput by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val countedUSD = countedUSDInput.toDoubleOrNull() ?: 0.0
    val countedFC = countedFCInput.toDoubleOrNull() ?: 0.0

    val discrepancyUSD = countedUSD - systemSalesUSD
    val discrepancyFC = countedFC - systemSalesFC

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clôture de Caisse Quotidienne") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Magasin : ${storeObj?.name ?: "Boutique"} • Vendeur : ${viewModel.activeStaffName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // System totals card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Ventes théoriques enregistrées aujourd'hui :", style = MaterialTheme.typography.labelSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total USD :", fontWeight = FontWeight.Bold)
                            Text("$ ${String.format("%.2f", systemSalesUSD)}", fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total FC :", fontWeight = FontWeight.Bold)
                            Text("${systemSalesFC.toInt()} FC", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("Espèces comptées physiquement dans le tiroir :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = countedUSDInput,
                        onValueChange = { countedUSDInput = it },
                        label = { Text("Total compté ($ USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("counted_usd_input")
                    )
                    OutlinedTextField(
                        value = countedFCInput,
                        onValueChange = { countedFCInput = it },
                        label = { Text("Total compté (FC)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("counted_fc_input")
                    )
                }

                // Discrepancy indicator
                if (countedUSDInput.isNotBlank() || countedFCInput.isNotBlank()) {
                    val hasDiscrepancy = Math.abs(discrepancyUSD) > 0.05 || Math.abs(discrepancyFC) > 100
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!hasDiscrepancy) ProfitGreen.copy(alpha = 0.12f) else LossRed.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (!hasDiscrepancy) "Caisse parfaitement équilibrée !" else "Écart constaté :",
                                fontWeight = FontWeight.Bold,
                                color = if (!hasDiscrepancy) ProfitGreen else LossRed,
                                fontSize = 12.sp
                            )
                            if (hasDiscrepancy) {
                                Text(
                                    text = "Écart USD : ${if (discrepancyUSD >= 0) "+$" else "-$"}${String.format("%.2f", Math.abs(discrepancyUSD))} | Écart FC : ${if (discrepancyFC >= 0) "+" else "-"}${Math.abs(discrepancyFC.toInt())} FC",
                                    fontWeight = FontWeight.Bold,
                                    color = LossRed,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Justification de l'écart / Remarques") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.recordCashClosing(
                        countedCashUSD = countedUSD,
                        countedCashFC = countedFC,
                        notes = notes,
                        onSuccess = onSuccess
                    )
                },
                modifier = Modifier.testTag("confirm_closing_btn")
            ) {
                Text("Enregistrer la clôture")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
