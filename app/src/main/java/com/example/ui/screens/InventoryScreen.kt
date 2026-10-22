package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.data.model.Product
import com.example.ui.components.AdjustExactStockDialog
import com.example.ui.components.ReportStockLossDialog
import com.example.ui.components.StockAlertBadge
import com.example.ui.components.StockMovementHistoryDialog
import com.example.ui.components.TransferStockDialog
import com.example.ui.state.KasaViewModel
import com.example.ui.theme.AlertWarning
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import java.text.DecimalFormat
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: KasaViewModel
) {
    // Collect reactive product stream directly from Room database
    val roomProductList by viewModel.roomProducts.collectAsState()
    val lowStockAlerts by viewModel.lowStockAlerts.collectAsState()
    val outOfStockAlerts by viewModel.outOfStockAlerts.collectAsState()
    val roomCategories by viewModel.roomCategories.collectAsState()
    val selectedStore by viewModel.selectedStore.collectAsState()

    var filterType by remember { mutableStateOf("ALL") } // "ALL", "LOW", "OUT", "NORMAL"
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state controllers
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var productForAdjust by remember { mutableStateOf<ProductEntity?>(null) }
    var productForHistory by remember { mutableStateOf<ProductEntity?>(null) }
    var showAllMovementsHistory by remember { mutableStateOf(false) }
    var productForTransfer by remember { mutableStateOf<Product?>(null) }
    var productForLoss by remember { mutableStateOf<Product?>(null) }

    // Filter products by active store, search, category, and stock health
    val storeFilteredProducts = remember(roomProductList, selectedStore) {
        if (selectedStore == null) {
            roomProductList
        } else {
            roomProductList.filter { it.storeId == selectedStore?.id || it.storeId.isBlank() }
        }
    }

    val filteredProducts = remember(storeFilteredProducts, filterType, selectedCategory, searchQuery) {
        storeFilteredProducts.filter { p ->
            val matchesFilter = when (filterType) {
                "LOW" -> p.isLowStock
                "OUT" -> p.isOutOfStock
                "NORMAL" -> !p.isLowStock && !p.isOutOfStock
                else -> true
            }
            val matchesCategory = selectedCategory == null || p.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesCategory && matchesSearch
        }
    }

    // High-impact inventory analytics
    val totalSKUs = storeFilteredProducts.size
    val totalUnits = remember(storeFilteredProducts) { storeFilteredProducts.sumOf { it.stockQuantity } }
    val totalValuationUSD = remember(storeFilteredProducts) { storeFilteredProducts.sumOf { it.stockQuantity * it.sellingPriceUSD } }
    val totalCostUSD = remember(storeFilteredProducts) { storeFilteredProducts.sumOf { it.stockQuantity * it.purchasePriceUSD } }
    val projectedProfitUSD = totalValuationUSD - totalCostUSD
    val urgentCount = lowStockAlerts.size + outOfStockAlerts.size

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inventory_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un produit")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nouvel Article", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Header Bar & Room Indicator
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gestion des Stocks & Catalogue",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ProfitGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Base Locale Room SQLite (Hors-ligne & Réactif)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ProfitGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Journal Movements CTA
                    OutlinedButton(
                        onClick = { showAllMovementsHistory = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("view_all_movements_button")
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Journal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Decorative Executive KPI Dashboard Cards
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Main Gradient Card: Stock Valuation
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Inventory,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Valeur Marchande du Stock",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "$totalSKUs références • ${totalUnits.toInt()} unités",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        Text(
                                            text = viewModel.formatMoney(totalValuationUSD),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = "Soit env. ${DecimalFormat("#,###").format(viewModel.convertUSDtoFC(totalValuationUSD))} FC",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }

                                    if (viewModel.isAdminSession && totalCostUSD > 0) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.TrendingUp,
                                                    contentDescription = null,
                                                    tint = ProfitGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "+${viewModel.formatMoney(projectedProfitUSD)}",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White
                                                )
                                            }
                                            Text(
                                                text = "Marge projetée",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Secondary Metric Cards: Low Stock & Out of Stock alerts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Alert Card 1: Low stock
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    filterType = if (filterType == "LOW") "ALL" else "LOW"
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (lowStockAlerts.isNotEmpty()) AlertWarning.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = if (filterType == "LOW") androidx.compose.foundation.BorderStroke(1.5.dp, AlertWarning) else null
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Stock Faible",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (lowStockAlerts.isNotEmpty()) AlertWarning else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (lowStockAlerts.isNotEmpty()) AlertWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${lowStockAlerts.size}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (lowStockAlerts.isNotEmpty()) AlertWarning else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (lowStockAlerts.isNotEmpty()) "À réapprovisionner" else "Stock suffisant",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Alert Card 2: Out of Stock
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    filterType = if (filterType == "OUT") "ALL" else "OUT"
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (outOfStockAlerts.isNotEmpty()) LossRed.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = if (filterType == "OUT") androidx.compose.foundation.BorderStroke(1.5.dp, LossRed) else null
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Rupture de Stock",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (outOfStockAlerts.isNotEmpty()) LossRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        Icons.Default.RemoveCircleOutline,
                                        contentDescription = null,
                                        tint = if (outOfStockAlerts.isNotEmpty()) LossRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${outOfStockAlerts.size}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (outOfStockAlerts.isNotEmpty()) LossRed else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (outOfStockAlerts.isNotEmpty()) "Ventes bloquées" else "Aucune rupture",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Urgent Restock Alert Banner
            if (urgentCount > 0 && filterType == "ALL") {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AlertWarning.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertWarning.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AlertWarning,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Alerte de Réapprovisionnement ($urgentCount articles)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${lowStockAlerts.size} articles sous le seuil d'alerte, ${outOfStockAlerts.size} épuisés.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { filterType = "LOW" },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertWarning),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Voir", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Search and Fast Barcode
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_search_input"),
                        placeholder = { Text("Rechercher par nom, catégorie, code-barres...") },
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
                }
            }

            // Category Horizontal Filter Chips
            if (roomCategories.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("Toutes catégories", fontSize = 12.sp) }
                        )
                        roomCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory.equals(cat, ignoreCase = true),
                                onClick = {
                                    selectedCategory = if (selectedCategory.equals(cat, ignoreCase = true)) null else cat
                                },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Stock Health Status Filter Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterType == "ALL",
                        onClick = { filterType = "ALL" },
                        label = { Text("Tous (${storeFilteredProducts.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterType == "LOW",
                        onClick = { filterType = "LOW" },
                        label = { Text("⚠️ Faible (${lowStockAlerts.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AlertWarning.copy(alpha = 0.2f),
                            selectedLabelColor = AlertWarning
                        )
                    )
                    FilterChip(
                        selected = filterType == "OUT",
                        onClick = { filterType = "OUT" },
                        label = { Text("🚨 Rupture (${outOfStockAlerts.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LossRed.copy(alpha = 0.2f),
                            selectedLabelColor = LossRed
                        )
                    )
                    FilterChip(
                        selected = filterType == "NORMAL",
                        onClick = { filterType = "NORMAL" },
                        label = { Text("✅ Normal (${storeFilteredProducts.count { !it.isLowStock && !it.isOutOfStock }})", fontSize = 11.sp) }
                    )
                }
            }

            // Product List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredProducts.size} article(s) affiché(s)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Products List or Empty State
            if (filteredProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aucun produit trouvé",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Créez votre premier article ou ajustez les filtres de recherche.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ajouter un article au catalogue")
                            }
                        }
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                        ProInventoryItemCard(
                            product = product,
                            viewModel = viewModel,
                            onEdit = { productToEdit = product },
                            onDelete = { viewModel.deleteProduct(product.id) },
                            onQuickRestock = { qty -> viewModel.quickRestock(product.id, qty) },
                            onAdjust = { productForAdjust = product },
                            onViewHistory = { productForHistory = product },
                            onTransfer = { productForTransfer = product.toProduct() },
                            onReportLoss = { productForLoss = product.toProduct() }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog with Room Support
    if (showAddDialog || productToEdit != null) {
        val initialEntity = productToEdit ?: ProductEntity(name = "")
        ProProductEditDialog(
            initialEntity = initialEntity,
            existingCategories = roomCategories,
            viewModel = viewModel,
            onDismiss = {
                showAddDialog = false
                productToEdit = null
            },
            onSave = {
                showAddDialog = false
                productToEdit = null
            }
        )
    }

    // Physical Inventory Counting & Adjustment Dialog
    productForAdjust?.let { entity ->
        AdjustExactStockDialog(
            product = entity,
            viewModel = viewModel,
            onDismiss = { productForAdjust = null },
            onSuccess = { productForAdjust = null }
        )
    }

    // Single Product Movements History
    productForHistory?.let { entity ->
        StockMovementHistoryDialog(
            viewModel = viewModel,
            targetProductId = entity.id,
            targetProductName = entity.name,
            onDismiss = { productForHistory = null }
        )
    }

    // All Movements Audit Trail Dialog
    if (showAllMovementsHistory) {
        StockMovementHistoryDialog(
            viewModel = viewModel,
            targetProductId = null,
            targetProductName = null,
            onDismiss = { showAllMovementsHistory = false }
        )
    }

    // Transfer Stock Dialog
    productForTransfer?.let { prod ->
        TransferStockDialog(
            product = prod,
            viewModel = viewModel,
            onDismiss = { productForTransfer = null },
            onSuccess = { productForTransfer = null }
        )
    }

    // Report Loss Dialog
    productForLoss?.let { prod ->
        ReportStockLossDialog(
            product = prod,
            viewModel = viewModel,
            onDismiss = { productForLoss = null },
            onSuccess = { productForLoss = null }
        )
    }
}

@Composable
private fun ProInventoryItemCard(
    product: ProductEntity,
    viewModel: KasaViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuickRestock: (Double) -> Unit,
    onAdjust: () -> Unit,
    onViewHistory: () -> Unit,
    onTransfer: () -> Unit,
    onReportLoss: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Progress bar for stock level health: ratio to alert threshold
    val alertThreshold = if (product.minStockAlert > 0) product.minStockAlert else 5.0
    val stockRatio = (product.stockQuantity / (alertThreshold * 2.0)).coerceIn(0.0, 1.0).toFloat()

    val (statusColor, statusBg, statusText) = when {
        product.isOutOfStock -> Triple(LossRed, LossRed.copy(alpha = 0.12f), "Rupture de Stock")
        product.isLowStock -> Triple(AlertWarning, AlertWarning.copy(alpha = 0.15f), "Stock Faible (Alerte)")
        else -> Triple(ProfitGreen, ProfitGreen.copy(alpha = 0.12f), "Stock Normal")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_item_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Name + Category chip + Menu/Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = product.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (product.barcode.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = product.barcode,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onViewHistory,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Historique",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Modifier",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = LossRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stock Health Gauge & Live Quantity Counter
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = statusBg
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = statusColor
                            )
                        }

                        Text(
                            text = "${product.stockQuantity.toInt()} ${product.unit}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { stockRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = statusColor.copy(alpha = 0.2f),
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Seuil critique d'alerte : < ${product.minStockAlert.toInt()} ${product.unit}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (product.stockQuantity > 0) {
                            Text(
                                text = "Valeur : ${viewModel.formatMoney(product.stockQuantity * product.sellingPriceUSD)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Price, Cost, and Profit Margin Metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Prix Vente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = viewModel.formatMoney(product.sellingPriceUSD),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (viewModel.isAdminSession) {
                    Column {
                        Text("Coût d'Achat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = viewModel.formatMoney(product.purchasePriceUSD),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Marge brute", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "+${viewModel.formatMoney(product.marginUSD)} (${product.marginPercent.toInt()}%)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Stock Adjustments [-1], [+1], [+5], [+10] & Inventory Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Secondary actions: Transfer & Loss & Exact Count
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onAdjust,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajuster", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onTransfer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Transférer", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onReportLoss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Perte", fontSize = 11.sp)
                    }
                }

                // Quick Stock Increment Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (product.stockQuantity > 0) {
                        Button(
                            onClick = { onQuickRestock(-1.0) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                        ) {
                            Text("-1", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { onQuickRestock(1.0) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                    ) {
                        Text("+1", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onQuickRestock(5.0) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                    ) {
                        Text("+5", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onQuickRestock(10.0) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                    ) {
                        Text("+10", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer l'article ?") },
            text = {
                Text(
                    "Êtes-vous sûr de vouloir retirer '${product.name}' du catalogue Room ?\n" +
                            if (product.stockQuantity > 0) "⚠️ Attention : il reste ${product.stockQuantity.toInt()} ${product.unit} en stock !" else ""
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Supprimer définitivement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun ProProductEditDialog(
    initialEntity: ProductEntity,
    existingCategories: List<String>,
    viewModel: KasaViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var name by remember { mutableStateOf(initialEntity.name) }
    var category by remember { mutableStateOf(initialEntity.category) }
    var barcode by remember { mutableStateOf(initialEntity.barcode) }
    var purchasePrice by remember { mutableStateOf(if (initialEntity.purchasePriceUSD > 0) initialEntity.purchasePriceUSD.toString() else "") }
    var sellingPrice by remember { mutableStateOf(if (initialEntity.sellingPriceUSD > 0) initialEntity.sellingPriceUSD.toString() else "") }
    var stockQuantity by remember { mutableStateOf(initialEntity.stockQuantity.toInt().toString()) }
    var minStockAlert by remember { mutableStateOf(initialEntity.minStockAlert.toInt().toString()) }
    var unit by remember { mutableStateOf(initialEntity.unit.ifBlank { "pcs" }) }
    var description by remember { mutableStateOf(initialEntity.description) }

    val isNew = initialEntity.id.isBlank() || initialEntity.name.isBlank()

    val pPriceVal = purchasePrice.toDoubleOrNull() ?: 0.0
    val sPriceVal = sellingPrice.toDoubleOrNull() ?: 0.0
    val liveMargin = sPriceVal - pPriceVal
    val liveMarginPct = if (pPriceVal > 0) (liveMargin / pPriceVal) * 100.0 else 0.0

    val unitSuggestions = listOf("pcs", "sac", "carton", "bidon", "boîte", "kg", "litre", "pack", "mètre")
    val defaultCategories = listOf("Alimentation", "Boissons & Laiterie", "Entretien & Hygiène", "Cosmétique", "Électronique", "Divers")
    val allCategories = remember(existingCategories) {
        (existingCategories + defaultCategories).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isNew) Icons.Default.Add else Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isNew) "Ajouter un nouvel article" else "Modifier la fiche article",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Enregistré dans la base SQLite locale Room",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Désignation de l'article *") },
                        singleLine = true,
                        placeholder = { Text("Ex: Riz Blanc 25kg") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_product_name")
                    )
                }

                // Category with suggestions
                item {
                    Column {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Catégorie *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            allCategories.take(6).forEach { cat ->
                                Surface(
                                    modifier = Modifier.clickable { category = cat },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (category.equals(cat, ignoreCase = true)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Pricing with Live Margin Preview
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = purchasePrice,
                                    onValueChange = { purchasePrice = it },
                                    label = { Text("Prix Achat ($) *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_purchase_price")
                                )
                                OutlinedTextField(
                                    value = sellingPrice,
                                    onValueChange = { sellingPrice = it },
                                    label = { Text("Prix Vente ($) *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_selling_price")
                                )
                            }

                            if (sPriceVal > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Marge calculée : +${DecimalFormat("$#,##0.00").format(liveMargin)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (liveMargin >= 0) ProfitGreen else LossRed
                                    )
                                    Text(
                                        text = "+${liveMarginPct.toInt()}% de rentabilité",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (liveMargin >= 0) ProfitGreen else LossRed
                                    )
                                }
                            }
                        }
                    }
                }

                // Stock and Alert Threshold
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stockQuantity,
                            onValueChange = { stockQuantity = it },
                            label = { Text("Stock initial *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_stock_qty")
                        )
                        OutlinedTextField(
                            value = minStockAlert,
                            onValueChange = { minStockAlert = it },
                            label = { Text("Seuil alerte *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_min_alert")
                        )
                    }
                }

                // Unit of measurement picker
                item {
                    Column {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unité de mesure (pcs, sac, carton, etc.)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            unitSuggestions.forEach { u ->
                                Surface(
                                    modifier = Modifier.clickable { unit = u },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (unit == u) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = u,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Barcode / SKU with Auto-generator button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Code barre / SKU") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                val prefix = name.take(3).uppercase().ifBlank { "ART" }
                                val rand = Random.nextInt(1000, 9999)
                                barcode = "$prefix-$rand"
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Générer", fontSize = 11.sp)
                        }
                    }
                }

                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Notes / Emplacement de stockage (optionnel)") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pPrice = purchasePrice.toDoubleOrNull() ?: 0.0
                    val sPrice = sellingPrice.toDoubleOrNull() ?: 0.0
                    val sQty = stockQuantity.toDoubleOrNull() ?: 0.0
                    val mAlert = minStockAlert.toDoubleOrNull() ?: 5.0

                    if (name.isNotBlank() && sPrice >= 0) {
                        viewModel.saveProduct(
                            id = initialEntity.id,
                            name = name,
                            category = category.ifBlank { "Général" },
                            barcode = barcode,
                            purchasePriceUSD = pPrice,
                            sellingPriceUSD = sPrice,
                            stockQuantity = sQty,
                            minStockAlert = mAlert,
                            unit = unit.ifBlank { "pcs" },
                            description = description,
                            onSuccess = onSave
                        )
                    }
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text(if (isNew) "Enregistrer dans Room" else "Mettre à jour")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
