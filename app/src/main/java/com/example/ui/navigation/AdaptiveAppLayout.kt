package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.CurrencyHeader
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.state.KasaViewModel

enum class NavDestination(val label: String, val icon: ImageVector) {
    DASHBOARD("Accueil", Icons.Default.Dashboard),
    POS("Caisse", Icons.Default.PointOfSale),
    INVENTORY("Stock", Icons.Default.Inventory2),
    CUSTOMERS("Dettes", Icons.Default.People),
    REPORTS("Rapports", Icons.Default.Assessment)
}

@Composable
fun AdaptiveAppLayout(
    viewModel: KasaViewModel,
    onSignOutClick: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(NavDestination.DASHBOARD) }
    val cartItems by viewModel.cartItems.collectAsState()
    val products by viewModel.products.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    // BackHandler: Return to Dashboard if on a sub-screen
    BackHandler(enabled = currentDestination != NavDestination.DASHBOARD) {
        currentDestination = NavDestination.DASHBOARD
    }

    val lowStockCount = products.count { it.isOutOfStock || it.isLowStock }
    val cartCount = cartItems.sumOf { it.quantity.toInt() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletLandscape = maxWidth >= 600.dp

        if (isTabletLandscape) {
            // Tablet layout: Side NavigationRail + Main Content
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NavDestination.values().forEach { destination ->
                        val isSelected = currentDestination == destination
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { currentDestination = destination },
                            icon = {
                                NavIconWithBadge(
                                    destination = destination,
                                    cartCount = cartCount,
                                    lowStockCount = lowStockCount
                                )
                            },
                            label = { Text(destination.label) },
                            modifier = Modifier.testTag("nav_${destination.name.lowercase()}")
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CurrencyHeader(
                        viewModel = viewModel,
                        onSignOutClick = onSignOutClick
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        ScreenContent(
                            destination = currentDestination,
                            viewModel = viewModel,
                            onNavigateTo = { currentDestination = it }
                        )
                    }
                }
            }
        } else {
            // Mobile layout: Top Header + Main Content + Bottom NavigationBar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    CurrencyHeader(
                        viewModel = viewModel,
                        onSignOutClick = onSignOutClick
                    )
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavDestination.values().forEach { destination ->
                            val isSelected = currentDestination == destination
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    NavIconWithBadge(
                                        destination = destination,
                                        cartCount = cartCount,
                                        lowStockCount = lowStockCount
                                    )
                                },
                                label = { Text(destination.label) },
                                modifier = Modifier.testTag("nav_${destination.name.lowercase()}")
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    ScreenContent(
                        destination = currentDestination,
                        viewModel = viewModel,
                        onNavigateTo = { currentDestination = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavIconWithBadge(
    destination: NavDestination,
    cartCount: Int,
    lowStockCount: Int
) {
    when (destination) {
        NavDestination.POS -> {
            if (cartCount > 0) {
                BadgedBox(badge = { Badge { Text("$cartCount") } }) {
                    Icon(destination.icon, contentDescription = destination.label)
                }
            } else {
                Icon(destination.icon, contentDescription = destination.label)
            }
        }
        NavDestination.INVENTORY -> {
            if (lowStockCount > 0) {
                BadgedBox(badge = { Badge { Text("$lowStockCount") } }) {
                    Icon(destination.icon, contentDescription = destination.label)
                }
            } else {
                Icon(destination.icon, contentDescription = destination.label)
            }
        }
        else -> {
            Icon(destination.icon, contentDescription = destination.label)
        }
    }
}

@Composable
private fun ScreenContent(
    destination: NavDestination,
    viewModel: KasaViewModel,
    onNavigateTo: (NavDestination) -> Unit
) {
    when (destination) {
        NavDestination.DASHBOARD -> DashboardScreen(
            viewModel = viewModel,
            onNavigateToPos = { onNavigateTo(NavDestination.POS) },
            onNavigateToInventory = { onNavigateTo(NavDestination.INVENTORY) },
            onNavigateToCustomers = { onNavigateTo(NavDestination.CUSTOMERS) },
            onNavigateToReports = { onNavigateTo(NavDestination.REPORTS) }
        )
        NavDestination.POS -> PosScreen(viewModel = viewModel)
        NavDestination.INVENTORY -> InventoryScreen(viewModel = viewModel)
        NavDestination.CUSTOMERS -> CustomersScreen(viewModel = viewModel)
        NavDestination.REPORTS -> ReportsScreen(viewModel = viewModel)
    }
}
