package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DetailedDebt
import com.example.data.model.Provision
import com.example.data.model.Requisition
import com.example.ui.state.KasaViewModel
import com.example.ui.theme.DebtBlue
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen

enum class CustomersTab(val label: String) {
    DEBTS("Dettes & Crédits"),
    PROVISIONS("Provisions (Commandes)"),
    REQUISITIONS("Réquisitions (Besoins)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: KasaViewModel
) {
    var selectedTab by remember { mutableStateOf(CustomersTab.DEBTS) }
    var searchQuery by remember { mutableStateOf("") }

    val debts = viewModel.getFilteredDebts()
    val provisions = viewModel.getFilteredProvisions()
    val requisitions = viewModel.getFilteredRequisitions()

    var showNewDebtDialog by remember { mutableStateOf(false) }
    var showNewProvisionDialog by remember { mutableStateOf(false) }
    var showNewRequisitionDialog by remember { mutableStateOf(false) }
    var debtForPayment by remember { mutableStateOf<DetailedDebt?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("customers_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        CustomersTab.DEBTS -> showNewDebtDialog = true
                        CustomersTab.PROVISIONS -> showNewProvisionDialog = true
                        CustomersTab.REQUISITIONS -> showNewRequisitionDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_item_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                CustomersTab.values().forEach { tab ->
                    val badgeCount = when (tab) {
                        CustomersTab.DEBTS -> debts.count { !it.isPaid }
                        CustomersTab.PROVISIONS -> provisions.count { it.status == "PENDING" }
                        CustomersTab.REQUISITIONS -> requisitions.size
                    }
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.label, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
                                if (badgeCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (selectedTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$badgeCount",
                                            color = if (selectedTab == tab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_input"),
                placeholder = {
                    Text(
                        when (selectedTab) {
                            CustomersTab.DEBTS -> "Rechercher par client, tél ou produit..."
                            CustomersTab.PROVISIONS -> "Rechercher une commande provisionnée..."
                            CustomersTab.REQUISITIONS -> "Rechercher un produit demandé..."
                        }
                    )
                },
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

            // Content according to selected tab
            when (selectedTab) {
                CustomersTab.DEBTS -> DebtsContent(
                    debts = debts.filter {
                        searchQuery.isBlank() ||
                                it.clientName.contains(searchQuery, ignoreCase = true) ||
                                it.clientPhone.contains(searchQuery) ||
                                it.productName.contains(searchQuery, ignoreCase = true)
                    },
                    viewModel = viewModel,
                    onPayClick = { debtForPayment = it }
                )
                CustomersTab.PROVISIONS -> ProvisionsContent(
                    provisions = provisions.filter {
                        searchQuery.isBlank() ||
                                it.clientName.contains(searchQuery, ignoreCase = true) ||
                                it.clientPhone.contains(searchQuery) ||
                                it.productName.contains(searchQuery, ignoreCase = true)
                    },
                    viewModel = viewModel
                )
                CustomersTab.REQUISITIONS -> RequisitionsContent(
                    requisitions = requisitions.filter {
                        searchQuery.isBlank() ||
                                it.productName.contains(searchQuery, ignoreCase = true) ||
                                it.category.contains(searchQuery, ignoreCase = true)
                    },
                    viewModel = viewModel
                )
            }
        }
    }

    // Modal: New Debt
    if (showNewDebtDialog) {
        NewDebtDialog(
            viewModel = viewModel,
            onDismiss = { showNewDebtDialog = false },
            onSuccess = { showNewDebtDialog = false }
        )
    }

    // Modal: Pay Debt
    debtForPayment?.let { debt ->
        PayDebtDialog(
            debt = debt,
            viewModel = viewModel,
            onDismiss = { debtForPayment = null },
            onSuccess = { debtForPayment = null }
        )
    }

    // Modal: New Provision
    if (showNewProvisionDialog) {
        NewProvisionDialog(
            viewModel = viewModel,
            onDismiss = { showNewProvisionDialog = false },
            onSuccess = { showNewProvisionDialog = false }
        )
    }

    // Modal: New Requisition
    if (showNewRequisitionDialog) {
        NewRequisitionDialog(
            viewModel = viewModel,
            onDismiss = { showNewRequisitionDialog = false },
            onSuccess = { showNewRequisitionDialog = false }
        )
    }
}

// -------------------------------------------------------------
// 1. DEBTS SECTION (Dettes & Acomptes)
// -------------------------------------------------------------

@Composable
private fun DebtsContent(
    debts: List<DetailedDebt>,
    viewModel: KasaViewModel,
    onPayClick: (DetailedDebt) -> Unit
) {
    val totalRemainingUSD = debts.sumOf { it.remainingUSD }
    val activeDebtorsCount = debts.count { !it.isPaid }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DebtBlue.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Encours Total des Dettes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$activeDebtorsCount créance(s) en attente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(viewModel.formatMoney(totalRemainingUSD), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DebtBlue)
                        Text(viewModel.formatMoney(totalRemainingUSD, "FC"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (debts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucune dette enregistrée pour le moment.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(debts, key = { it.id }) { debt ->
                DebtCardItem(debt = debt, viewModel = viewModel, onPayClick = { onPayClick(debt) })
            }
        }
    }
}

@Composable
private fun DebtCardItem(
    debt: DetailedDebt,
    viewModel: KasaViewModel,
    onPayClick: () -> Unit
) {
    val context = LocalContext.current

    val statusBadgeColor = when {
        debt.isPaid -> ProfitGreen
        debt.isPartial -> Color(0xFFF57C00) // Orange / Avance
        else -> LossRed
    }

    val statusLabel = when {
        debt.isPaid -> "PAYÉ"
        debt.isPartial -> "AVANCE (Reste dû)"
        else -> "NON PAYÉ"
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("debt_card_${debt.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(statusBadgeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = statusBadgeColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(debt.clientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (debt.clientPhone.isNotBlank()) {
                            Text(debt.clientPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBadgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(statusLabel, color = statusBadgeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Debt details
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Article(s) : ${debt.productName} (${debt.quantity.toInt()} ${debt.unit})", style = MaterialTheme.typography.bodyMedium)
                    if (debt.dueDate.isNotBlank()) {
                        Text("Échéance : ${debt.dueDate}", style = MaterialTheme.typography.bodySmall, color = LossRed, fontWeight = FontWeight.Medium)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total : ${viewModel.formatMoney(debt.totalAmountUSD)}", style = MaterialTheme.typography.bodySmall)
                    if (debt.advancePaidUSD > 0.01) {
                        Text("Acompte : ${viewModel.formatMoney(debt.advancePaidUSD)}", style = MaterialTheme.typography.bodySmall, color = ProfitGreen)
                    }
                    Text("Reste : ${viewModel.formatMoney(debt.remainingUSD)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (debt.isPaid) ProfitGreen else LossRed)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call button if phone exists
                if (debt.clientPhone.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${debt.clientPhone}"))
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Appeler", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Share debt reminder on WhatsApp
                    IconButton(
                        onClick = {
                            val reminderText = "Bonjour ${debt.clientName}, rappel amical concernant votre achat de '${debt.productName}'. Total: ${viewModel.formatMoney(debt.totalAmountUSD)}, Acompte: ${viewModel.formatMoney(debt.advancePaidUSD)}, Reste à payer: ${viewModel.formatMoney(debt.remainingUSD)} avant le ${debt.dueDate}. Merci !"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, reminderText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Rappel WhatsApp"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Rappel WhatsApp", tint = ProfitGreen)
                    }
                }

                if (!debt.isPaid) {
                    Button(
                        onClick = onPayClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verser un acompte")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. PROVISIONS SECTION (Commandes Payées d'Avance)
// -------------------------------------------------------------

@Composable
private fun ProvisionsContent(
    provisions: List<Provision>,
    viewModel: KasaViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Commandes & Provisions d'Avance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Clients ayant versé un acompte pour des articles à commander. Dès réception, convertissez en vente livrée d'un clic !",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (provisions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("Aucune provision ou commande en attente.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(provisions, key = { it.id }) { prov ->
                ProvisionCardItem(provision = prov, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ProvisionCardItem(
    provision: Provision,
    viewModel: KasaViewModel
) {
    val isPending = provision.status == "PENDING"
    val statusColor = if (isPending) Color(0xFFF57C00) else ProfitGreen

    Card(
        modifier = Modifier.fillMaxWidth().testTag("provision_card_${provision.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(provision.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Client : ${provision.clientName} (${provision.clientPhone})", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(if (isPending) "EN ATTENTE" else "LIVRÉ", color = statusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Quantité commandée : ${provision.quantity.toInt()} ${provision.unit}", style = MaterialTheme.typography.bodyMedium)
                    if (provision.dueDate.isNotBlank()) {
                        Text("Échéance convenue : ${provision.dueDate}", style = MaterialTheme.typography.bodySmall, color = LossRed, fontWeight = FontWeight.Bold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Acompte reçu : ${viewModel.formatMoney(provision.advanceAmountUSD)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ProfitGreen)
                    if (provision.totalPriceUSD > 0) {
                        Text("Prix convenu : ${viewModel.formatMoney(provision.totalPriceUSD)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (isPending) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.deliverProvision(provision, onSuccess = {})
                    },
                    modifier = Modifier.fillMaxWidth().testTag("deliver_provision_btn_${provision.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Article arrivé : Convertir en Vente Livrée", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. REQUISITIONS SECTION (Besoins Clients Non Satisfaits avec Vote +1)
// -------------------------------------------------------------

@Composable
private fun RequisitionsContent(
    requisitions: List<Requisition>,
    viewModel: KasaViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cahier des Besoins Clients (Réquisitions)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Produits demandés par les clients et introuvables en boutique. Cliquez sur '+1 Demande' à chaque nouveau client demandeur pour prioriser vos prochains approvisionnements.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (requisitions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("Aucun besoin client enregistré.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(requisitions, key = { it.id }) { req ->
                RequisitionCardItem(requisition = req, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun RequisitionCardItem(
    requisition: Requisition,
    viewModel: KasaViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("req_card_${requisition.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(requisition.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Rayon : ${requisition.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (requisition.notes.isNotBlank()) {
                    Text("Note : ${requisition.notes}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${requisition.requestCount} demande(s)",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { viewModel.incrementRequisition(requisition.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "+1", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+1")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODALS & DIALOGS
// -------------------------------------------------------------

@Composable
private fun NewDebtDialog(
    viewModel: KasaViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("pcs") }
    var totalAmountInput by remember { mutableStateOf("") }
    var advancePaidInput by remember { mutableStateOf("0") }
    var currencyUsed by remember { mutableStateOf("USD") }
    var dueDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle Dette Client") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nom complet du client *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Numéro téléphone (WhatsApp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Article / Marchandise prêtée *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange = { quantityInput = it },
                            label = { Text("Quantité") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unité") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = currencyUsed == "USD", onClick = { currencyUsed = "USD" })
                        Text("USD ($)")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = currencyUsed == "FC", onClick = { currencyUsed = "FC" })
                        Text("FC")
                    }
                }
                item {
                    OutlinedTextField(
                        value = totalAmountInput,
                        onValueChange = { totalAmountInput = it },
                        label = { Text("Montant total de la dette ($currencyUsed) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = advancePaidInput,
                        onValueChange = { advancePaidInput = it },
                        label = { Text("Acompte versé ($currencyUsed)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Date d'échéance (ex: 30/10/2026)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Observations") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val totalRaw = totalAmountInput.toDoubleOrNull() ?: 0.0
                    val advRaw = advancePaidInput.toDoubleOrNull() ?: 0.0

                    val totalUSD = if (currencyUsed == "USD") totalRaw else viewModel.convertFCtoUSD(totalRaw)
                    val advUSD = if (currencyUsed == "USD") advRaw else viewModel.convertFCtoUSD(advRaw)

                    if (clientName.isNotBlank() && productName.isNotBlank() && totalUSD > 0) {
                        viewModel.saveDetailedDebt(
                            clientName = clientName.trim(),
                            clientPhone = clientPhone.trim(),
                            productName = productName.trim(),
                            quantity = quantityInput.toDoubleOrNull() ?: 1.0,
                            unit = unit.trim(),
                            totalAmountUSD = totalUSD,
                            advancePaidUSD = advUSD,
                            dueDate = dueDate.trim(),
                            notes = notes.trim(),
                            onSuccess = onSuccess
                        )
                    }
                }
            ) {
                Text("Enregistrer la dette")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun PayDebtDialog(
    debt: DetailedDebt,
    viewModel: KasaViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var paymentInput by remember { mutableStateOf("") }
    var currencyUsed by remember { mutableStateOf("USD") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Versement sur dette") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Client : ${debt.clientName}", fontWeight = FontWeight.Bold)
                Text("Article : ${debt.productName}")
                Text("Solde restant : ${viewModel.formatMoney(debt.remainingUSD)} (${viewModel.formatMoney(debt.remainingUSD, "FC")})", color = LossRed, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = currencyUsed == "USD", onClick = { currencyUsed = "USD" })
                    Text("USD ($)")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = currencyUsed == "FC", onClick = { currencyUsed = "FC" })
                    Text("FC")
                }

                OutlinedTextField(
                    value = paymentInput,
                    onValueChange = { paymentInput = it },
                    label = { Text("Montant versé ($currencyUsed)") },
                    placeholder = { Text(if (currencyUsed == "USD") "${debt.remainingUSD}" else "${debt.remainingFC.toInt()}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (ex: Reçu en cash)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = paymentInput.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        viewModel.payDetailedDebt(
                            debt = debt,
                            paymentAmount = amount,
                            currencyUsed = currencyUsed,
                            notes = notes,
                            onSuccess = onSuccess
                        )
                    }
                }
            ) {
                Text("Valider le versement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun NewProvisionDialog(
    viewModel: KasaViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("pcs") }
    var advanceAmountInput by remember { mutableStateOf("") }
    var totalPriceInput by remember { mutableStateOf("") }
    var currencyUsed by remember { mutableStateOf("USD") }
    var dueDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle Provision (Commande Payée d'Avance)") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nom du client *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Téléphone WhatsApp *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Produit commandé *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange = { quantityInput = it },
                            label = { Text("Quantité") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unité") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = currencyUsed == "USD", onClick = { currencyUsed = "USD" })
                        Text("USD ($)")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = currencyUsed == "FC", onClick = { currencyUsed = "FC" })
                        Text("FC")
                    }
                }
                item {
                    OutlinedTextField(
                        value = advanceAmountInput,
                        onValueChange = { advanceAmountInput = it },
                        label = { Text("Acompte / Montant versé ($currencyUsed) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = totalPriceInput,
                        onValueChange = { totalPriceInput = it },
                        label = { Text("Prix total convenu ($currencyUsed)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Date de livraison prévue (ex: 29/10/2026)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val advRaw = advanceAmountInput.toDoubleOrNull() ?: 0.0
                    val totRaw = totalPriceInput.toDoubleOrNull() ?: advRaw

                    val advUSD = if (currencyUsed == "USD") advRaw else viewModel.convertFCtoUSD(advRaw)
                    val totUSD = if (currencyUsed == "USD") totRaw else viewModel.convertFCtoUSD(totRaw)

                    if (clientName.isNotBlank() && productName.isNotBlank() && advUSD > 0) {
                        viewModel.saveProvision(
                            clientName = clientName.trim(),
                            clientPhone = clientPhone.trim(),
                            productName = productName.trim(),
                            quantity = quantityInput.toDoubleOrNull() ?: 1.0,
                            unit = unit.trim(),
                            advanceAmountUSD = advUSD,
                            totalPriceUSD = totUSD,
                            dueDate = dueDate.trim(),
                            onSuccess = onSuccess
                        )
                    }
                }
            ) {
                Text("Enregistrer la commande")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun NewRequisitionDialog(
    viewModel: KasaViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Alimentation") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau Produit Demandé (Réquisition)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Nom du produit introuvable *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Catégorie / Rayon") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Précisions (marque, format, prix client souhaité)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (productName.isNotBlank()) {
                        viewModel.saveRequisition(
                            productName = productName.trim(),
                            category = category.trim(),
                            notes = notes.trim(),
                            onSuccess = onSuccess
                        )
                    }
                }
            ) {
                Text("Enregistrer le besoin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
