package com.example.ui.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CashClosing
import com.example.data.model.Customer
import com.example.data.model.DebtPayment
import com.example.data.model.DetailedDebt
import com.example.data.model.Expense
import com.example.data.model.InviteCode
import com.example.data.model.Product
import com.example.data.model.Provision
import com.example.data.model.Requisition
import com.example.data.local.AppDatabase
import com.example.data.local.ProductRoomRepository
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.StaffMember
import com.example.data.model.Store
import com.example.data.model.UserSettings
import com.example.data.repository.KasaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class KasaViewModel(
    private val repository: KasaRepository,
    private val roomRepository: ProductRoomRepository
) : ViewModel() {

    constructor(context: Context) : this(
        KasaRepository(context),
        ProductRoomRepository(
            AppDatabase.getInstance(context).productDao(),
            AppDatabase.getInstance(context).stockMovementDao()
        )
    )

    // --- Room Database Streams (Local Persistence & Realtime Stock Tracking) ---
    val roomProducts: StateFlow<List<ProductEntity>> = roomRepository.allProducts
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockAlerts: StateFlow<List<ProductEntity>> = roomRepository.lowStockProducts
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outOfStockAlerts: StateFlow<List<ProductEntity>> = roomRepository.outOfStockProducts
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roomCategories: StateFlow<List<String>> = roomRepository.allCategories
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentStockMovements: StateFlow<List<StockMovementEntity>> = roomRepository.recentMovements
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Realtime Streams from Firestore
    val stores: StateFlow<List<Store>> = repository.observeStores()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val staffMembers: StateFlow<List<StaffMember>> = repository.observeStaff()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inviteCodes: StateFlow<List<InviteCode>> = repository.observeInviteCodes()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashClosings: StateFlow<List<CashClosing>> = repository.observeCashClosings()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val detailedDebts: StateFlow<List<DetailedDebt>> = repository.observeDetailedDebts()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val provisions: StateFlow<List<Provision>> = repository.observeProvisions()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val requisitions: StateFlow<List<Requisition>> = repository.observeRequisitions()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = repository.observeProducts()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<Sale>> = repository.observeSales()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = repository.observeExpenses()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<UserSettings> = repository.observeSettings()
        .catch { emit(UserSettings()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    // Active Cart for POS
    private val _cartItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val cartItems: StateFlow<List<SaleItem>> = _cartItems.asStateFlow()

    // Multi-store filter (null = "Toutes les succursales / Consolidé")
    private val _selectedStore = MutableStateFlow<Store?>(null)
    val selectedStore: StateFlow<Store?> = _selectedStore.asStateFlow()

    // Current Staff Session (null = Administrateur / Gérant Propriétaire)
    private val _currentStaff = MutableStateFlow<StaffMember?>(null)
    val currentStaff: StateFlow<StaffMember?> = _currentStaff.asStateFlow()

    // Active display currency override (or defaults to settings)
    private val _displayCurrency = MutableStateFlow<String?>(null)
    val displayCurrency: StateFlow<String?> = _displayCurrency.asStateFlow()

    // User message feedback
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Last completed sale (for WhatsApp receipt sharing)
    private val _lastCompletedSale = MutableStateFlow<Sale?>(null)
    val lastCompletedSale: StateFlow<Sale?> = _lastCompletedSale.asStateFlow()

    init {
        // Auto-seed default stores if empty
        viewModelScope.launch {
            stores.collect { list ->
                if (list.isEmpty()) {
                    repository.seedInitialDataIfEmpty(list.size)
                }
            }
        }

        // Bi-directional sync: synchronize Firestore products into Room database
        viewModelScope.launch {
            products.collect { cloudList ->
                if (cloudList.isNotEmpty()) {
                    roomRepository.syncCloudProductsToRoom(cloudList)
                }
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun clearLastSale() {
        _lastCompletedSale.value = null
    }

    fun selectStore(store: Store?) {
        _selectedStore.value = store
    }

    val isAdminSession: Boolean
        get() = _currentStaff.value == null || _currentStaff.value?.isAdmin == true

    val activeStaffName: String
        get() = _currentStaff.value?.name ?: "Gérant (Admin)"

    fun switchStaffWithPin(enteredPin: String): Boolean {
        if (enteredPin == "0000" || enteredPin == "1234") {
            // Admin default PIN
            _currentStaff.value = null
            _userMessage.value = "Connecté en tant qu'Administrateur"
            return true
        }
        val staff = staffMembers.value.firstOrNull { it.pinCode == enteredPin && it.isActive }
        return if (staff != null) {
            _currentStaff.value = staff
            // Automatically switch store to staff's assigned store
            val matchingStore = stores.value.firstOrNull { it.id == staff.storeId }
            if (matchingStore != null) {
                _selectedStore.value = matchingStore
            }
            _userMessage.value = "Connecté : ${staff.name} (${staff.storeName})"
            true
        } else {
            _userMessage.value = "Code PIN invalide"
            false
        }
    }

    fun switchStaff(staff: StaffMember?) {
        _currentStaff.value = staff
        if (staff != null) {
            val matchingStore = stores.value.firstOrNull { it.id == staff.storeId }
            if (matchingStore != null) {
                _selectedStore.value = matchingStore
            }
        }
    }

    // --- Currency & Formatting ---

    fun setDisplayCurrency(currency: String) {
        _displayCurrency.value = currency
    }

    fun getEffectiveCurrency(): String {
        return _displayCurrency.value ?: settings.value.primaryCurrency
    }

    fun formatMoney(amountUSD: Double, forceCurrency: String? = null): String {
        val currency = forceCurrency ?: getEffectiveCurrency()
        val rate = settings.value.exchangeRate
        return if (currency == "FC") {
            val amountFC = amountUSD * rate
            val formatter = DecimalFormat("#,###")
            "${formatter.format(amountFC)} FC"
        } else {
            val formatter = DecimalFormat("#,##0.00")
            "$ ${formatter.format(amountUSD)}"
        }
    }

    fun convertUSDtoFC(amountUSD: Double): Double {
        return amountUSD * settings.value.exchangeRate
    }

    fun convertFCtoUSD(amountFC: Double): Double {
        val rate = settings.value.exchangeRate
        return if (rate > 0) amountFC / rate else 0.0
    }

    fun getFilteredProducts(): List<Product> {
        val store = _selectedStore.value
        val sourceList = if (roomProducts.value.isNotEmpty()) {
            roomProducts.value.map { it.toProduct() }
        } else {
            products.value
        }
        return if (store == null) {
            sourceList
        } else {
            sourceList.filter { it.storeId == store.id || it.storeId.isBlank() }
        }
    }

    fun getFilteredProductEntities(): List<ProductEntity> {
        val store = _selectedStore.value
        val list = roomProducts.value
        return if (store == null) {
            list
        } else {
            list.filter { it.storeId == store.id || it.storeId.isBlank() }
        }
    }

    fun getFilteredSales(): List<Sale> {
        val store = _selectedStore.value
        return if (store == null) {
            sales.value
        } else {
            sales.value.filter { it.storeId == store.id || it.storeId.isBlank() }
        }
    }

    fun getFilteredDebts(): List<DetailedDebt> {
        val store = _selectedStore.value
        return if (store == null) {
            detailedDebts.value
        } else {
            detailedDebts.value.filter { it.storeId == store.id || it.storeId.isBlank() }
        }
    }

    fun getFilteredProvisions(): List<Provision> {
        val store = _selectedStore.value
        return if (store == null) {
            provisions.value
        } else {
            provisions.value.filter { it.storeId == store.id || it.storeId.isBlank() }
        }
    }

    fun getFilteredRequisitions(): List<Requisition> {
        val store = _selectedStore.value
        return if (store == null) {
            requisitions.value
        } else {
            requisitions.value.filter { it.storeId == store.id || it.storeId.isBlank() }
        }
    }

    // --- POS Cart Operations ---

    fun addToCart(product: Product) {
        val current = _cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.productId == product.id }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            val newQty = existing.quantity + 1.0
            current[existingIndex] = existing.copy(
                quantity = newQty,
                totalUSD = newQty * existing.unitPriceUSD
            )
        } else {
            current.add(
                SaleItem(
                    productId = product.id,
                    productName = product.name,
                    quantity = 1.0,
                    unitPriceUSD = product.sellingPriceUSD,
                    purchasePriceUSD = product.purchasePriceUSD,
                    totalUSD = product.sellingPriceUSD
                )
            )
        }
        _cartItems.value = current
    }

    fun updateCartItemQuantity(productId: String, newQuantity: Double) {
        if (newQuantity <= 0) {
            removeFromCart(productId)
            return
        }
        val current = _cartItems.value.map { item ->
            if (item.productId == productId) {
                item.copy(quantity = newQuantity, totalUSD = newQuantity * item.unitPriceUSD)
            } else item
        }
        _cartItems.value = current
    }

    fun removeFromCart(productId: String) {
        _cartItems.value = _cartItems.value.filterNot { it.productId == productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    val cartTotalUSD: Double
        get() = _cartItems.value.sumOf { it.totalUSD }

    val cartTotalGrossProfitUSD: Double
        get() = _cartItems.value.sumOf { it.profitUSD }

    fun processSale(
        paymentType: String,
        currencyUsed: String,
        customerId: String = "",
        customerName: String = "",
        customerPhone: String = "",
        onSuccess: () -> Unit
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) return

        val rate = settings.value.exchangeRate
        val totalUSD = cartTotalUSD
        val totalFC = totalUSD * rate
        val grossProfitUSD = cartTotalGrossProfitUSD

        val currentStoreObj = _selectedStore.value ?: stores.value.firstOrNull()

        val sale = Sale(
            storeId = currentStoreObj?.id ?: "",
            storeName = currentStoreObj?.name ?: "Boutique Principale",
            staffName = activeStaffName,
            items = items.map {
                mapOf(
                    "productId" to it.productId,
                    "productName" to it.productName,
                    "quantity" to it.quantity,
                    "unitPriceUSD" to it.unitPriceUSD,
                    "purchasePriceUSD" to it.purchasePriceUSD,
                    "totalUSD" to it.totalUSD
                )
            },
            totalUSD = totalUSD,
            totalFC = totalFC,
            currencyUsed = currencyUsed,
            exchangeRate = rate,
            grossProfitUSD = grossProfitUSD,
            customerId = customerId,
            customerName = customerName,
            customerPhone = customerPhone,
            paymentType = paymentType,
            isPaid = paymentType == "CASH"
        )

        viewModelScope.launch {
            val result = repository.recordSale(sale, items)
            // Deduct stock in Room database and log stock audit movement
            for (item in items) {
                roomRepository.adjustStock(
                    productId = item.productId,
                    delta = -item.quantity,
                    reason = "Vente caisse #${sale.id.take(8)}",
                    operator = activeStaffName,
                    movementType = "SALE"
                )
            }
            if (result.isSuccess) {
                _lastCompletedSale.value = sale
                clearCart()
                _userMessage.value = "Vente enregistrée avec succès !"
                onSuccess()
            } else {
                _userMessage.value = "Erreur lors de l'enregistrement de la vente"
            }
        }
    }

    // --- Receipt Generator (WhatsApp / SMS Ready) ---

    fun buildWhatsAppReceipt(sale: Sale, receivedAmount: Double = sale.totalUSD): String {
        val rate = sale.exchangeRate
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date())

        val sb = StringBuilder()
        sb.append("🧾 *REÇU DE VENTE - ${sale.storeName.ifBlank { "KASAPOS" }}*\n")
        sb.append("📅 *Date :* $dateStr\n")
        if (sale.customerName.isNotBlank()) {
            sb.append("👤 *Client :* ${sale.customerName}" + if (sale.customerPhone.isNotBlank()) " (${sale.customerPhone})" else "" + "\n")
        }
        sb.append("👨‍💼 *Caissier :* ${sale.staffName}\n")
        sb.append("------------------------------------\n")

        for (item in sale.items) {
            val name = item["productName"] as? String ?: "Article"
            val qty = (item["quantity"] as? Number)?.toDouble() ?: 1.0
            val total = (item["totalUSD"] as? Number)?.toDouble() ?: 0.0
            sb.append("• ${qty.toInt()}x $name : $ ${DecimalFormat("#,##0.00").format(total)} (${DecimalFormat("#,###").format(total * rate)} FC)\n")
        }

        sb.append("------------------------------------\n")
        sb.append("💰 *TOTAL À PAYER :* $ ${DecimalFormat("#,##0.00").format(sale.totalUSD)} (${DecimalFormat("#,###").format(sale.totalFC)} FC)\n")

        if (sale.paymentType == "CASH") {
            val change = (receivedAmount - sale.totalUSD).coerceAtLeast(0.0)
            if (change > 0) {
                sb.append("💵 *Reçu :* $ ${DecimalFormat("#,##0.00").format(receivedAmount)}\n")
                sb.append("🔄 *Monnaie rendue :* $ ${DecimalFormat("#,##0.00").format(change)} (${DecimalFormat("#,###").format(change * rate)} FC)\n")
            }
            sb.append("✅ *Mode :* Au comptant (Payé)\n")
        } else {
            sb.append("⚠️ *Mode :* À Crédit (Dette client enregistrée)\n")
        }

        sb.append("------------------------------------\n")
        sb.append("🙏 *Merci pour votre fidélité !*\n")
        return sb.toString()
    }

    // --- Product & Inventory Management (Room Database + Cloud Sync) ---

    fun saveProduct(
        id: String = "",
        storeId: String = "",
        storeName: String = "",
        name: String,
        category: String,
        barcode: String,
        purchasePriceUSD: Double,
        sellingPriceUSD: Double,
        stockQuantity: Double,
        minStockAlert: Double,
        unit: String,
        description: String = "",
        onSuccess: () -> Unit
    ) {
        val currentStoreObj = if (storeId.isNotBlank()) {
            stores.value.firstOrNull { it.id == storeId }
        } else {
            _selectedStore.value ?: stores.value.firstOrNull()
        }

        viewModelScope.launch {
            val entity = ProductEntity(
                id = if (id.isNotBlank()) id else UUID.randomUUID().toString(),
                storeId = currentStoreObj?.id ?: "",
                storeName = currentStoreObj?.name ?: "Boutique Principale",
                name = name.trim(),
                category = category.trim().ifBlank { "Général" },
                barcode = barcode.trim(),
                purchasePriceUSD = purchasePriceUSD,
                sellingPriceUSD = sellingPriceUSD,
                stockQuantity = stockQuantity,
                minStockAlert = minStockAlert,
                unit = unit.trim().ifBlank { "pcs" },
                description = description.trim()
            )

            // Save in Room Database first (offline-first & reactive)
            val finalId = roomRepository.saveProduct(entity, operator = activeStaffName)

            // Sync to Firestore cloud database
            try {
                repository.saveProduct(entity.copy(id = finalId).toProduct())
            } catch (_: Exception) {}

            _userMessage.value = "Article sauvegardé dans le catalogue Room !"
            onSuccess()
        }
    }

    fun quickRestock(productId: String, addedQty: Double) {
        viewModelScope.launch {
            roomRepository.adjustStock(
                productId = productId,
                delta = addedQty,
                reason = "Réapprovisionnement rapide (+${addedQty.toInt()})",
                operator = activeStaffName,
                movementType = "ENTRY"
            )
            try {
                repository.adjustProductStock(productId, addedQty)
            } catch (_: Exception) {}
            _userMessage.value = "Stock réapprovisionné (+${addedQty.toInt()}) dans Room"
        }
    }

    fun adjustExactStock(
        productId: String,
        exactStock: Double,
        reason: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentProd = roomProducts.value.firstOrNull { it.id == productId }
            roomRepository.setExactStock(
                productId = productId,
                exactStock = exactStock,
                reason = reason.ifBlank { "Inventaire physique" },
                operator = activeStaffName
            )
            val currentQty = currentProd?.stockQuantity ?: 0.0
            val delta = exactStock - currentQty
            if (delta != 0.0) {
                try {
                    repository.adjustProductStock(productId, delta)
                } catch (_: Exception) {}
            }
            _userMessage.value = "Stock ajusté à ${exactStock.toInt()} (Mouvement consigné)"
            onSuccess()
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            roomRepository.deleteProduct(productId)
            try {
                repository.deleteProduct(productId)
            } catch (_: Exception) {}
            _userMessage.value = "Article retiré du catalogue Room"
        }
    }

    fun getMovementsForProduct(productId: String) = roomRepository.getMovementsForProduct(productId)

    // --- Transfert de stock & Pertes ---

    fun transferStock(
        product: Product,
        toStore: Store,
        quantity: Double,
        onSuccess: () -> Unit
    ) {
        val fromStoreObj = stores.value.firstOrNull { it.id == product.storeId }
            ?: _selectedStore.value
            ?: Store(id = "", name = "Boutique Source")

        viewModelScope.launch {
            val res = repository.transferStock(
                product = product,
                fromStoreId = fromStoreObj.id,
                fromStoreName = fromStoreObj.name,
                toStoreId = toStore.id,
                toStoreName = toStore.name,
                quantity = quantity,
                staffName = activeStaffName
            )
            if (res.isSuccess) {
                _userMessage.value = "Transfert de ${quantity.toInt()} ${product.unit} vers ${toStore.name} réussi !"
                onSuccess()
            }
        }
    }

    fun recordStockLoss(
        product: Product,
        quantity: Double,
        reason: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.recordStockLoss(
                product = product,
                quantity = quantity,
                reason = reason,
                staffName = activeStaffName
            )
            if (res.isSuccess) {
                _userMessage.value = "Perte déclarée : ${quantity.toInt()} ${product.unit} ($reason)"
                onSuccess()
            }
        }
    }

    // --- Clôture de Caisse Quotidienne ---

    fun recordCashClosing(
        countedCashUSD: Double,
        countedCashFC: Double,
        notes: String,
        onSuccess: () -> Unit
    ) {
        val storeObj = _selectedStore.value ?: stores.value.firstOrNull()
        val todayStoreSales = getFilteredSales().filter { isToday(it.createdAt) }

        val systemSalesUSD = todayStoreSales.filter { it.currencyUsed == "USD" }.sumOf { it.totalUSD }
        val systemSalesFC = todayStoreSales.filter { it.currencyUsed == "FC" }.sumOf { it.totalFC }

        val discrepancyUSD = countedCashUSD - systemSalesUSD
        val discrepancyFC = countedCashFC - systemSalesFC

        val closing = CashClosing(
            storeId = storeObj?.id ?: "",
            storeName = storeObj?.name ?: "Boutique",
            staffName = activeStaffName,
            systemSalesUSD = systemSalesUSD,
            systemSalesFC = systemSalesFC,
            countedCashUSD = countedCashUSD,
            countedCashFC = countedCashFC,
            discrepancyUSD = discrepancyUSD,
            discrepancyFC = discrepancyFC,
            notes = notes
        )

        viewModelScope.launch {
            val res = repository.recordCashClosing(closing)
            if (res.isSuccess) {
                _userMessage.value = "Clôture de caisse validée et enregistrée !"
                onSuccess()
            }
        }
    }

    // --- Dettes Détaillées par Article & Acomptes ---

    fun saveDetailedDebt(
        clientName: String,
        clientPhone: String,
        productName: String,
        quantity: Double,
        unit: String,
        totalAmountUSD: Double,
        advancePaidUSD: Double,
        dueDate: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        val storeObj = _selectedStore.value ?: stores.value.firstOrNull()
        val rate = settings.value.exchangeRate
        val totalAmountFC = totalAmountUSD * rate
        val advancePaidFC = advancePaidUSD * rate

        viewModelScope.launch {
            val debt = DetailedDebt(
                storeId = storeObj?.id ?: "",
                storeName = storeObj?.name ?: "Boutique",
                clientName = clientName,
                clientPhone = clientPhone,
                productName = productName,
                quantity = quantity,
                unit = unit,
                totalAmountUSD = totalAmountUSD,
                totalAmountFC = totalAmountFC,
                advancePaidUSD = advancePaidUSD,
                advancePaidFC = advancePaidFC,
                dueDate = dueDate,
                staffName = activeStaffName,
                notes = notes
            )
            val res = repository.saveDetailedDebt(debt)
            if (res.isSuccess) {
                _userMessage.value = "Dette enregistrée avec acompte de ${formatMoney(advancePaidUSD)}"
                onSuccess()
            }
        }
    }

    fun payDetailedDebt(
        debt: DetailedDebt,
        paymentAmount: Double,
        currencyUsed: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        val paymentUSD = if (currencyUsed == "USD") paymentAmount else convertFCtoUSD(paymentAmount)
        val paymentFC = if (currencyUsed == "FC") paymentAmount else convertUSDtoFC(paymentAmount)

        viewModelScope.launch {
            val res = repository.payDetailedDebt(debt.id, paymentUSD, paymentFC, notes)
            if (res.isSuccess) {
                _userMessage.value = "Versement de dette de ${formatMoney(paymentUSD)} validé !"
                onSuccess()
            }
        }
    }

    // --- Provisions (Commandes Payées d'Avance) ---

    fun saveProvision(
        clientName: String,
        clientPhone: String,
        productName: String,
        quantity: Double,
        unit: String,
        advanceAmountUSD: Double,
        totalPriceUSD: Double,
        dueDate: String,
        onSuccess: () -> Unit
    ) {
        val storeObj = _selectedStore.value ?: stores.value.firstOrNull()
        val rate = settings.value.exchangeRate
        val advanceAmountFC = advanceAmountUSD * rate

        viewModelScope.launch {
            val provision = Provision(
                storeId = storeObj?.id ?: "",
                storeName = storeObj?.name ?: "Boutique",
                clientName = clientName,
                clientPhone = clientPhone,
                productName = productName,
                quantity = quantity,
                unit = unit,
                advanceAmountUSD = advanceAmountUSD,
                advanceAmountFC = advanceAmountFC,
                totalPriceUSD = totalPriceUSD,
                dueDate = dueDate,
                status = "PENDING",
                staffName = activeStaffName
            )
            val res = repository.saveProvision(provision)
            if (res.isSuccess) {
                _userMessage.value = "Provision / Acompte de commande enregistré !"
                onSuccess()
            }
        }
    }

    fun deliverProvision(
        provision: Provision,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.convertProvisionToSale(
                provision = provision,
                exchangeRate = settings.value.exchangeRate,
                profitUSD = provision.totalPriceUSD * 0.2 // Estimated margin if unrecorded
            )
            if (res.isSuccess) {
                _userMessage.value = "Provision livrée et convertie en vente au comptant !"
                onSuccess()
            }
        }
    }

    // --- Réquisitions (Besoins Clients Non Satisfaits avec Vote +1) ---

    fun saveRequisition(
        productName: String,
        category: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        val storeObj = _selectedStore.value ?: stores.value.firstOrNull()
        viewModelScope.launch {
            val req = Requisition(
                storeId = storeObj?.id ?: "",
                storeName = storeObj?.name ?: "Boutique",
                productName = productName,
                category = category,
                requestCount = 1,
                notes = notes
            )
            val res = repository.saveRequisition(req)
            if (res.isSuccess) {
                _userMessage.value = "Besoin client enregistré !"
                onSuccess()
            }
        }
    }

    fun incrementRequisition(requisitionId: String) {
        viewModelScope.launch {
            repository.incrementRequisitionCount(requisitionId)
            _userMessage.value = "+1 demande client ajoutée !"
        }
    }

    // --- Store & Invite Code Management ---

    fun addStore(name: String, address: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.createStore(name, address)
            _userMessage.value = "Nouvelle succursale '$name' créée"
            onSuccess()
        }
    }

    fun generateInviteCode(store: Store, role: String, onGenerated: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.generateInviteCode(store.id, store.name, role)
            if (res.isSuccess) {
                val code = res.getOrThrow()
                _userMessage.value = "Code d'accès employé généré : $code"
                onGenerated(code)
            }
        }
    }

    fun registerStaffWithCode(
        code: String,
        name: String,
        phone: String,
        pin: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.registerStaffWithInviteCode(code, name, phone, pin)
            if (res.isSuccess) {
                val member = res.getOrThrow()
                _currentStaff.value = member
                _userMessage.value = "Bienvenue ${member.name} dans ${member.storeName} !"
                onSuccess()
            } else {
                _userMessage.value = res.exceptionOrNull()?.message ?: "Échec de l'inscription"
            }
        }
    }

    // --- Expenses ---

    fun saveExpense(
        id: String = "",
        title: String,
        category: String,
        amount: Double,
        currencyUsed: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        val rate = settings.value.exchangeRate
        val amountUSD = if (currencyUsed == "USD") amount else convertFCtoUSD(amount)
        val amountFC = if (currencyUsed == "FC") amount else convertUSDtoFC(amount)

        viewModelScope.launch {
            val expense = Expense(
                id = id,
                title = title,
                category = category,
                amountUSD = amountUSD,
                amountFC = amountFC,
                currencyUsed = currencyUsed,
                notes = notes
            )
            val result = repository.saveExpense(expense)
            if (result.isSuccess) {
                _userMessage.value = "Dépense enregistrée"
                onSuccess()
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            _userMessage.value = "Dépense supprimée"
        }
    }

    // --- Settings & Exchange Rate ---

    fun updateExchangeRate(newRate: Double) {
        if (newRate <= 0) return
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(exchangeRate = newRate))
            _userMessage.value = "Taux de change mis à jour (1$ = ${newRate.toInt()} FC)"
        }
    }

    fun updateShopName(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(shopName = newName.trim()))
        }
    }

    fun isToday(timestamp: com.google.firebase.Timestamp?): Boolean {
        if (timestamp == null) return false
        val cal1 = Calendar.getInstance()
        cal1.time = timestamp.toDate()
        val cal2 = Calendar.getInstance()
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isWithinDays(timestamp: com.google.firebase.Timestamp?, days: Int): Boolean {
        if (timestamp == null) return false
        val millis = timestamp.toDate().time
        val now = System.currentTimeMillis()
        val diff = now - millis
        return diff <= days * 24L * 60L * 60L * 1000L
    }
}
