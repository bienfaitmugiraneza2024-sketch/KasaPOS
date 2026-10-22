package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Store
import com.example.ui.state.KasaViewModel

@Composable
fun CurrencyHeader(
    viewModel: KasaViewModel,
    onSignOutClick: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val selectedStore by viewModel.selectedStore.collectAsState()
    val currentStaff by viewModel.currentStaff.collectAsState()
    val currentCurrency = viewModel.getEffectiveCurrency()

    var showStoreDropdown by remember { mutableStateOf(false) }
    var showAddStoreDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showSwitchStaffDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showCashClosingDialog by remember { mutableStateOf(false) }
    var showRegisterStaffDialog by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Store branch selector dropdown & Staff session pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Store branch selector button
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { showStoreDropdown = true }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("store_selector_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedStore?.name ?: "Toutes succursales",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showStoreDropdown,
                            onDismissRequest = { showStoreDropdown = false }
                        ) {
                            if (viewModel.isAdminSession) {
                                DropdownMenuItem(
                                    text = { Text("Toutes les succursales (Consolidé)", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        viewModel.selectStore(null)
                                        showStoreDropdown = false
                                    }
                                )
                            }
                            stores.forEach { store ->
                                DropdownMenuItem(
                                    text = { Text(store.name) },
                                    onClick = {
                                        viewModel.selectStore(store)
                                        showStoreDropdown = false
                                    }
                                )
                            }
                            if (viewModel.isAdminSession) {
                                DropdownMenuItem(
                                    text = { Text("+ Nouvelle succursale", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showStoreDropdown = false
                                        showAddStoreDialog = true
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Staff user badge (clickable for PIN switch)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (viewModel.isAdminSession) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            )
                            .clickable { showSwitchStaffDialog = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("staff_session_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (viewModel.isAdminSession) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (viewModel.isAdminSession) "Gérant" else (currentStaff?.name ?: "Vendeur"),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (viewModel.isAdminSession) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Currency switcher & exchange rate & actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Exchange rate button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showRateDialog = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("rate_header_btn")
                    ) {
                        Text(
                            text = "1$=${settings.exchangeRate.toInt()}FC",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // USD / FC Toggle
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            CurrencyTogglePill(
                                label = "$",
                                isSelected = currentCurrency == "USD",
                                onClick = { viewModel.setDisplayCurrency("USD") },
                                testTag = "currency_usd"
                            )
                            CurrencyTogglePill(
                                label = "FC",
                                isSelected = currentCurrency == "FC",
                                onClick = { viewModel.setDisplayCurrency("FC") },
                                testTag = "currency_fc"
                            )
                        }
                    }

                    // Admin Actions & Cash Closing
                    if (viewModel.isAdminSession) {
                        IconButton(
                            onClick = { showInviteDialog = true },
                            modifier = Modifier.size(36.dp).testTag("invite_staff_btn")
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Inviter un employé",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showCashClosingDialog = true },
                        modifier = Modifier.size(36.dp).testTag("cash_closing_btn")
                    ) {
                        Icon(
                            Icons.Default.PointOfSale,
                            contentDescription = "Clôture de caisse",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onSignOutClick,
                        modifier = Modifier.size(36.dp).testTag("sign_out_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Déconnexion",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showSwitchStaffDialog) {
        SwitchStaffDialog(
            viewModel = viewModel,
            onDismiss = { showSwitchStaffDialog = false },
            onRegisterClick = {
                showSwitchStaffDialog = false
                showRegisterStaffDialog = true
            }
        )
    }

    if (showRegisterStaffDialog) {
        RegisterStaffWithCodeDialog(
            viewModel = viewModel,
            onDismiss = { showRegisterStaffDialog = false },
            onSuccess = { showRegisterStaffDialog = false }
        )
    }

    if (showInviteDialog) {
        InviteEmployeeDialog(
            viewModel = viewModel,
            onDismiss = { showInviteDialog = false }
        )
    }

    if (showCashClosingDialog) {
        CashClosingDialog(
            viewModel = viewModel,
            onDismiss = { showCashClosingDialog = false },
            onSuccess = { showCashClosingDialog = false }
        )
    }

    if (showAddStoreDialog) {
        var storeName by remember { mutableStateOf("") }
        var storeAddress by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddStoreDialog = false },
            title = { Text("Nouvelle succursale") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nom du magasin *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_store_name")
                    )
                    OutlinedTextField(
                        value = storeAddress,
                        onValueChange = { storeAddress = it },
                        label = { Text("Adresse / Emplacement") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (storeName.isNotBlank()) {
                            viewModel.addStore(storeName, storeAddress) {
                                showAddStoreDialog = false
                            }
                        }
                    }
                ) {
                    Text("Créer la succursale")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStoreDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showRateDialog) {
        var inputRate by remember { mutableStateOf(settings.exchangeRate.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            title = { Text("Taux de change (FC / USD)") },
            text = {
                Column {
                    Text(
                        "Réglez le cours du Franc Congolais pour 1 Dollar américain. Toutes les conversions seront instantanément recalculées.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputRate,
                        onValueChange = { inputRate = it },
                        label = { Text("1 USD = X Francs Congolais") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("exchange_rate_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = inputRate.toDoubleOrNull()
                        if (rate != null && rate > 0) {
                            viewModel.updateExchangeRate(rate)
                            showRateDialog = false
                        }
                    }
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRateDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun CurrencyTogglePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
