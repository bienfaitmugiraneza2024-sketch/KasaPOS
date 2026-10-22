package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.StockAlertBadge
import com.example.ui.state.KasaViewModel
import com.example.ui.theme.DebtBlue
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: KasaViewModel
) {
    val products = viewModel.getFilteredProducts()
    val cartItems by viewModel.cartItems.collectAsState()
    val lastCompletedSale by viewModel.lastCompletedSale.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tous") }
    var showCartSheet by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    val categories = remember(products) {
        listOf("Tous") + products.map { it.category.ifBlank { "Général" } }.distinct().sorted()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { product ->
            val matchQuery = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.barcode.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategory == "Tous" || product.category.equals(selectedCategory, ignoreCase = true)
            matchQuery && matchCategory
        }
    }

    Box(modifier = Modifier.fillMaxSize().testTag("pos_screen")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (cartItems.isNotEmpty()) 80.dp else 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("pos_search_input"),
                placeholder = { Text("Rechercher un article ou code barre...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Products Grid
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun produit ne correspond à votre recherche.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductPosCard(
                            product = product,
                            viewModel = viewModel,
                            onAddToCart = { viewModel.addToCart(product) }
                        )
                    }
                }
            }
        }

        // Bottom Cart Bar
        if (cartItems.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCartSheet = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${cartItems.sumOf { it.quantity.toInt() }} article(s) dans le panier",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                            Text(
                                text = viewModel.formatMoney(viewModel.cartTotalUSD),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Button(
                        onClick = { showCheckoutDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("checkout_button")
                    ) {
                        Text("Encaisser", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Cart Details
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Panier de vente",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text("Vider le panier", color = LossRed)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                cartItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.productName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "${viewModel.formatMoney(item.unitPriceUSD)} / unité",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quantity +/- controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.updateCartItemQuantity(item.productId, item.quantity - 1.0) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Diminuer", modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "${item.quantity.toInt()}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { viewModel.updateCartItemQuantity(item.productId, item.quantity + 1.0) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Augmenter", modifier = Modifier.size(18.dp))
                            }
                        }

                        Text(
                            text = viewModel.formatMoney(item.totalUSD),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total en USD :", style = MaterialTheme.typography.bodyMedium)
                            Text(viewModel.formatMoney(viewModel.cartTotalUSD, "USD"), fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total en Francs Congolais :", style = MaterialTheme.typography.bodyMedium)
                            Text(viewModel.formatMoney(viewModel.cartTotalUSD, "FC"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Profit brut estimé :", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(viewModel.formatMoney(viewModel.cartTotalGrossProfitUSD), color = ProfitGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        showCartSheet = false
                        showCheckoutDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Passer au règlement (${viewModel.formatMoney(viewModel.cartTotalUSD)})", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Checkout & Payment Dialog
    if (showCheckoutDialog) {
        CheckoutDialog(
            viewModel = viewModel,
            onDismiss = { showCheckoutDialog = false },
            onSuccess = {
                showCheckoutDialog = false
            }
        )
    }

    // WhatsApp Receipt Dialog
    lastCompletedSale?.let { sale ->
        ReceiptDialog(
            sale = sale,
            viewModel = viewModel,
            onDismiss = { viewModel.clearLastSale() }
        )
    }
}

@Composable
private fun ProductPosCard(
    product: Product,
    viewModel: KasaViewModel,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onAddToCart)
            .testTag("pos_product_card_${product.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            StockAlertBadge(product = product)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = viewModel.formatMoney(product.sellingPriceUSD),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            // Counter currency equivalent
            val otherCurr = if (viewModel.getEffectiveCurrency() == "USD") "FC" else "USD"
            Text(
                text = "≈ ${viewModel.formatMoney(product.sellingPriceUSD, otherCurr)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onAddToCart,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Ajouter",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun CheckoutDialog(
    viewModel: KasaViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var paymentType by remember { mutableStateOf("CASH") } // "CASH" or "DEBT"
    var currencyUsed by remember { mutableStateOf(viewModel.getEffectiveCurrency()) } // "USD" or "FC"
    var receivedAmountInput by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var advancePaidInput by remember { mutableStateOf("") }

    val totalUSD = viewModel.cartTotalUSD
    val totalFC = viewModel.convertUSDtoFC(totalUSD)
    val requiredAmount = if (currencyUsed == "USD") totalUSD else totalFC

    val receivedAmount = receivedAmountInput.toDoubleOrNull() ?: requiredAmount
    val changeAmount = (receivedAmount - requiredAmount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Règlement de la vente") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Payment Mode Selection: Comptant vs Dette
                Text("Mode de règlement :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { paymentType = "CASH" }
                    ) {
                        RadioButton(selected = paymentType == "CASH", onClick = { paymentType = "CASH" })
                        Text("Au comptant (Cash)")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { paymentType = "DEBT" }
                    ) {
                        RadioButton(selected = paymentType == "DEBT", onClick = { paymentType = "DEBT" })
                        Text("À crédit (Dette)")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Currency Choice
                Text("Devise de paiement :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { currencyUsed = "USD" }
                    ) {
                        RadioButton(selected = currencyUsed == "USD", onClick = { currencyUsed = "USD" })
                        Text("Dollar ($ USD)")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { currencyUsed = "FC" }
                    ) {
                        RadioButton(selected = currencyUsed == "FC", onClick = { currencyUsed = "FC" })
                        Text("Franc Congolais (FC)")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Client Info (useful for receipt and debt)
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Nom du client (Optionnel pour cash, requis pour dette)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("debt_customer_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Numéro téléphone (WhatsApp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (paymentType == "CASH") {
                    OutlinedTextField(
                        value = receivedAmountInput,
                        onValueChange = { receivedAmountInput = it },
                        label = { Text("Montant reçu ($currencyUsed)") },
                        placeholder = { Text(if (currencyUsed == "USD") "$totalUSD" else "${totalFC.toInt()}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("received_amount_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (changeAmount > 0.01) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ProfitGreen.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Monnaie à rendre au client :",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ProfitGreen
                                )
                                Text(
                                    text = if (currencyUsed == "USD") {
                                        "$ ${String.format("%.2f", changeAmount)} (ou ${(changeAmount * viewModel.settings.value.exchangeRate).toInt()} FC)"
                                    } else {
                                        "${changeAmount.toInt()} FC (ou $ ${String.format("%.2f", changeAmount / viewModel.settings.value.exchangeRate)})"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ProfitGreen
                                )
                            }
                        }
                    }
                } else {
                    // Credit details
                    OutlinedTextField(
                        value = advancePaidInput,
                        onValueChange = { advancePaidInput = it },
                        label = { Text("Acompte versé aujourd'hui ($currencyUsed)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Date d'échéance (ex: 28/10/2026)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val client = customerName.ifBlank { if (paymentType == "DEBT") "Client Crédit" else "Client comptant" }
                    val items = viewModel.cartItems.value
                    val productSummary = items.joinToString(", ") { "${it.quantity.toInt()}x ${it.productName}" }

                    viewModel.processSale(
                        paymentType = paymentType,
                        currencyUsed = currencyUsed,
                        customerName = client,
                        customerPhone = customerPhone.trim(),
                        onSuccess = {
                            if (paymentType == "DEBT") {
                                val advInput = advancePaidInput.toDoubleOrNull() ?: 0.0
                                val advUSD = if (currencyUsed == "USD") advInput else viewModel.convertFCtoUSD(advInput)
                                viewModel.saveDetailedDebt(
                                    clientName = client,
                                    clientPhone = customerPhone.trim(),
                                    productName = productSummary,
                                    quantity = items.sumOf { it.quantity },
                                    unit = "pcs",
                                    totalAmountUSD = totalUSD,
                                    advancePaidUSD = advUSD,
                                    dueDate = dueDate.ifBlank { "Non spécifiée" },
                                    notes = "Vente caisse à crédit",
                                    onSuccess = {}
                                )
                            }
                            onSuccess()
                        }
                    )
                },
                modifier = Modifier.testTag("confirm_sale_button")
            ) {
                Text("Valider la vente")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
