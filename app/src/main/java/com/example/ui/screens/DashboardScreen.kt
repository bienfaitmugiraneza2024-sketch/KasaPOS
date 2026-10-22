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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.ui.components.StatCard
import com.example.ui.components.StockAlertBadge
import com.example.ui.state.KasaViewModel
import com.example.ui.theme.AlertWarning
import com.example.ui.theme.CobaltSecondaryLight
import com.example.ui.theme.DebtBlue
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen

@Composable
fun DashboardScreen(
    viewModel: KasaViewModel,
    onNavigateToPos: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val sales by viewModel.sales.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val products by viewModel.products.collectAsState()
    val debts by viewModel.detailedDebts.collectAsState()

    // Real-time Daily Calculations
    val todaySales = sales.filter { viewModel.isToday(it.createdAt) }
    val todayRevenueUSD = todaySales.sumOf { it.totalUSD }
    val todayGrossProfitUSD = todaySales.sumOf { it.grossProfitUSD }

    val todayExpenses = expenses.filter { viewModel.isToday(it.createdAt) }
    val todayExpensesUSD = todayExpenses.sumOf { it.amountUSD }

    val todayNetProfitUSD = todayGrossProfitUSD - todayExpensesUSD
    val totalCustomerDebtUSD = debts.sumOf { it.remainingUSD }

    val criticalProducts = products.filter { it.isOutOfStock || it.isLowStock }
    val outOfStockCount = products.count { it.isOutOfStock }
    val lowStockCount = products.count { it.isLowStock }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Action Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToPos,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_action_pos"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nouvelle Vente", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigateToInventory,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_action_inventory"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gérer Stock", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live Profit & Financial KPIs
        item {
            Text(
                text = "Performance du Jour (En direct)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // CA du jour
                    StatCard(
                        title = "Ventes Aujourd'hui",
                        primaryValue = viewModel.formatMoney(todayRevenueUSD),
                        secondaryValue = "${todaySales.size} vente(s)",
                        icon = Icons.Default.AttachMoney,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    // Dépenses du jour
                    StatCard(
                        title = "Dépenses Aujourd'hui",
                        primaryValue = viewModel.formatMoney(todayExpensesUSD),
                        secondaryValue = "${todayExpenses.size} charge(s)",
                        icon = Icons.Default.MoneyOff,
                        iconTint = LossRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Bénéfice Net
                    val isProfitPositive = todayNetProfitUSD >= 0
                    StatCard(
                        title = "Bénéfice Net Réel",
                        primaryValue = viewModel.formatMoney(todayNetProfitUSD),
                        secondaryValue = "Marge brute: ${viewModel.formatMoney(todayGrossProfitUSD)}",
                        icon = Icons.Default.TrendingUp,
                        iconTint = if (isProfitPositive) ProfitGreen else LossRed,
                        containerColor = if (isProfitPositive) ProfitGreen.copy(alpha = 0.08f) else LossRed.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f)
                    )

                    // Dettes clients
                    StatCard(
                        title = "Dettes Clients En Cours",
                        primaryValue = viewModel.formatMoney(totalCustomerDebtUSD),
                        secondaryValue = "${debts.count { !it.isPaid }} créance(s) active(s)",
                        icon = Icons.Default.CreditCard,
                        iconTint = DebtBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Stock Alerts Card (if any item low or out of stock)
        item {
            if (criticalProducts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stock_alerts_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AlertWarning.copy(alpha = 0.12f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AlertWarning.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = AlertWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Alertes de Stock (${criticalProducts.size})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$outOfStockCount en rupture, $lowStockCount stock faible",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = onNavigateToInventory,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertWarning)
                            ) {
                                Text("Voir tout", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Preview top 3 critical products with quick restock
                        criticalProducts.take(3).forEach { product ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    StockAlertBadge(product = product)
                                }

                                Button(
                                    onClick = { viewModel.quickRestock(product.id, 10.0) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("+10 ${product.unit}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ProfitGreen.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ProfitGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tous les niveaux de stock sont optimaux !",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ProfitGreen
                        )
                    }
                }
            }
        }

        // Recent Sales List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Dernières Ventes Enregistrées",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Voir rapports",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onNavigateToReports)
                )
            }
        }

        if (sales.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aucune vente enregistrée pour le moment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(sales.take(5)) { sale ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (sale.paymentType == "DEBT") "Vente à crédit (${sale.customerName})" else "Vente au comptant",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (sale.paymentType == "DEBT") DebtBlue.copy(alpha = 0.15f) else ProfitGreen.copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = sale.paymentType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sale.paymentType == "DEBT") DebtBlue else ProfitGreen
                                    )
                                }
                            }
                            Text(
                                text = "${sale.items.size} article(s) • Taux: 1$ = ${sale.exchangeRate.toInt()} FC",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = viewModel.formatMoney(sale.totalUSD),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "+${viewModel.formatMoney(sale.grossProfitUSD)} profit",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = ProfitGreen
                            )
                        }
                    }
                }
            }
        }
    }
}
