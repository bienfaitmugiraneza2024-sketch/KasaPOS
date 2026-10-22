package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.Store
import com.example.ui.state.KasaViewModel
import com.example.ui.theme.LossRed

@Composable
fun TransferStockDialog(
    product: Product,
    viewModel: KasaViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val stores by viewModel.stores.collectAsState()
    val availableDestinationStores = stores.filter { it.id != product.storeId }
    var selectedDestStore by remember { mutableStateOf(availableDestinationStores.firstOrNull()) }
    var transferQtyInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transférer du stock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Article : ${product.name} (Stock actuel : ${product.stockQuantity.toInt()} ${product.unit})",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                Text("Transférer vers quelle succursale :", style = MaterialTheme.typography.labelMedium)

                if (availableDestinationStores.isEmpty()) {
                    Text(
                        text = "Aucune autre succursale disponible. Créez d'abord un autre magasin.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(110.dp)) {
                        items(availableDestinationStores) { store ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDestStore = store }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedDestStore?.id == store.id,
                                    onClick = { selectedDestStore = store }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(store.name, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = transferQtyInput,
                    onValueChange = { transferQtyInput = it },
                    label = { Text("Quantité à transférer (${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("transfer_qty_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = transferQtyInput.toDoubleOrNull()
                    val dest = selectedDestStore
                    if (dest != null && qty != null && qty > 0) {
                        if (qty <= product.stockQuantity) {
                            viewModel.transferStock(product, dest, qty, onSuccess)
                        } else {
                            errorMessage = "Quantité supérieure au stock disponible !"
                        }
                    } else {
                        errorMessage = "Veuillez entrer une quantité valide."
                    }
                },
                enabled = availableDestinationStores.isNotEmpty(),
                modifier = Modifier.testTag("confirm_transfer_btn")
            ) {
                Text("Valider le transfert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun ReportStockLossDialog(
    product: Product,
    viewModel: KasaViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var lossQtyInput by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf("Casse / Bouteille brisée") }
    val reasons = listOf("Casse / Bouteille brisée", "Produit périmé", "Marchandise avariée / mouillée", "Vol constaté", "Défaut d'emballage")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Déclarer une Perte / Avarie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Article : ${product.name} (Stock actuel : ${product.stockQuantity.toInt()} ${product.unit})",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = lossQtyInput,
                    onValueChange = { lossQtyInput = it },
                    label = { Text("Quantité perdue (${product.unit}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("loss_qty_input")
                )

                Text("Motif de la perte :", style = MaterialTheme.typography.labelMedium)

                LazyColumn(modifier = Modifier.height(130.dp)) {
                    items(reasons) { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(reason, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = lossQtyInput.toDoubleOrNull()
                    if (qty != null && qty > 0) {
                        viewModel.recordStockLoss(product, qty, selectedReason, onSuccess)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                modifier = Modifier.testTag("confirm_loss_btn")
            ) {
                Text("Déduire la perte")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
