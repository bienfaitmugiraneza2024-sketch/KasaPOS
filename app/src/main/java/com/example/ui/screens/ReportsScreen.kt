package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.ui.components.StatCard
import com.example.ui.state.KasaViewModel
import com.example.ui.theme.CobaltSecondaryLight
import com.example.ui.theme.DebtBlue
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen

@Composable
fun ReportsScreen(
    viewModel: KasaViewModel
) {
    val sales by viewModel.sales.collectAsState()
    val expenses by viewModel.expenses.collectAsState()

    var periodFilter by remember { mutableStateOf("TODAY") } // "TODAY", "WEEK", "MONTH", "ALL"
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    // Filter by period
    val periodSales = remember(sales, periodFilter) {
        when (periodFilter) {
            "TODAY" -> sales.filter { viewModel.isToday(it.createdAt) }
            "WEEK" -> sales.filter { viewModel.isWithinDays(it.createdAt, 7) }
            "MONTH" -> sales.filter { viewModel.isWithinDays(it.createdAt, 30) }
            else -> sales
        }
    }

    val periodExpenses = remember(expenses, periodFilter) {
        when (periodFilter) {
            "TODAY" -> expenses.filter { viewModel.isToday(it.createdAt) }
            "WEEK" -> expenses.filter { viewModel.isWithinDays(it.createdAt, 7) }
            "MONTH" -> expenses.filter { viewModel.isWithinDays(it.createdAt, 30) }
            else -> expenses
        }
    }

    // Calculations
    val totalRevenueUSD = periodSales.sumOf { it.totalUSD }
    val totalGrossProfitUSD = periodSales.sumOf { it.grossProfitUSD }
    val totalCostOfGoodsUSD = (totalRevenueUSD - totalGrossProfitUSD).coerceAtLeast(0.0)
    val totalExpensesUSD = periodExpenses.sumOf { it.amountUSD }
    val netProfitUSD = totalGrossProfitUSD - totalExpensesUSD

    // Expenses breakdown by category
    val expensesByCategory = remember(periodExpenses) {
        periodExpenses.groupBy { it.category.ifBlank { "Divers" } }
            .mapValues { entry -> entry.value.sumOf { it.amountUSD } }
            .toList()
            .sortedByDescending { it.second }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("reports_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Selector Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = periodFilter == "TODAY",
                        onClick = { periodFilter = "TODAY" },
                        label = { Text("Aujourd'hui") }
                    )
                    FilterChip(
                        selected = periodFilter == "WEEK",
                        onClick = { periodFilter = "WEEK" },
                        label = { Text("7 jours") }
                    )
                    FilterChip(
                        selected = periodFilter == "MONTH",
                        onClick = { periodFilter = "MONTH" },
                        label = { Text("Ce mois") }
                    )
                    FilterChip(
                        selected = periodFilter == "ALL",
                        onClick = { periodFilter = "ALL" },
                        label = { Text("Global") }
                    )
                }
            }

            // P&L Statement Card (Compte de résultat)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Bilan Financier & Rentabilité",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Line: Ventes
                        ReportMetricLine(
                            label = "(+) Chiffre d'Affaires (Ventes)",
                            value = viewModel.formatMoney(totalRevenueUSD),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Line: Coût Marchandises
                        ReportMetricLine(
                            label = "(-) Coût d'Achat des Marchandises",
                            value = "-${viewModel.formatMoney(totalCostOfGoodsUSD)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                                .padding(vertical = 4.dp)
                        )

                        // Line: Marge Brute
                        ReportMetricLine(
                            label = "(=) Marge Commerciale Brute",
                            value = viewModel.formatMoney(totalGrossProfitUSD),
                            color = ProfitGreen,
                            isBold = true
                        )

                        // Line: Dépenses
                        ReportMetricLine(
                            label = "(-) Dépenses d'Exploitation",
                            value = "-${viewModel.formatMoney(totalExpensesUSD)}",
                            color = LossRed
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Big Net Profit Highlight
                        val isPositive = netProfitUSD >= 0
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPositive) ProfitGreen.copy(alpha = 0.15f) else LossRed.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isPositive) ProfitGreen else LossRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "BÉNÉFICE NET RÉEL",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isPositive) ProfitGreen else LossRed
                                        )
                                        Text(
                                            text = if (isPositive) "Résultat bénéficiaire" else "Déficit d'exploitation",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = viewModel.formatMoney(netProfitUSD),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPositive) ProfitGreen else LossRed
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Expenses Breakdown Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Répartition des Dépenses (${viewModel.formatMoney(totalExpensesUSD)})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Button(
                        onClick = { showAddExpenseDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.testTag("add_expense_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter dépense", fontSize = 11.sp)
                    }
                }
            }

            if (expensesByCategory.isEmpty()) {
                item {
                    Text(
                        text = "Aucune dépense enregistrée sur cette période.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(expensesByCategory) { (category, amount) ->
                    val fraction = if (totalExpensesUSD > 0) (amount / totalExpensesUSD).toFloat() else 0f
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = category, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${viewModel.formatMoney(amount)} (${(fraction * 100).toInt()}%)",
                                    fontWeight = FontWeight.Bold,
                                    color = LossRed
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = LossRed,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            // Detailed Expenses List
            item {
                Text(
                    text = "Historique des Charges Enregistrées",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (periodExpenses.isEmpty()) {
                item {
                    Text("Aucune charge récente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(periodExpenses) { expense ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = expense.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    text = "${expense.category}" + if (expense.notes.isNotBlank()) " • ${expense.notes}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "-${viewModel.formatMoney(expense.amountUSD)}",
                                    fontWeight = FontWeight.Bold,
                                    color = LossRed
                                )
                                IconButton(onClick = { viewModel.deleteExpense(expense.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = LossRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            viewModel = viewModel,
            onDismiss = { showAddExpenseDialog = false },
            onSuccess = { showAddExpenseDialog = false }
        )
    }
}

@Composable
private fun ReportMetricLine(
    label: String,
    value: String,
    color: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold),
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    viewModel: KasaViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Loyer") }
    var amountInput by remember { mutableStateOf("") }
    var currencyUsed by remember { mutableStateOf("USD") } // "USD" or "FC"
    var notes by remember { mutableStateOf("") }

    val categories = listOf("Loyer", "Transport", "Électricité / Eau", "Salaires", "Emballages / Sacs", "Taxes & Impôts", "Divers")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enregistrer une Dépense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Motif de la dépense *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_title")
                )

                // Category selector
                Text("Catégorie :", style = MaterialTheme.typography.labelSmall)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                // Currency selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Devise :", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = currencyUsed == "USD", onClick = { currencyUsed = "USD" })
                    Text("USD")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = currencyUsed == "FC", onClick = { currencyUsed = "FC" })
                    Text("FC")
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Montant ($currencyUsed) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_amount")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes explicatives (Optionnel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountInput.toDoubleOrNull()
                    if (title.isNotBlank() && amount != null && amount > 0) {
                        viewModel.saveExpense(
                            title = title,
                            category = category,
                            amount = amount,
                            currencyUsed = currencyUsed,
                            notes = notes,
                            onSuccess = onSuccess
                        )
                    }
                },
                modifier = Modifier.testTag("save_expense_confirm_button")
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
