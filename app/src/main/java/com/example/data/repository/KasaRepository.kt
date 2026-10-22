package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CashClosing
import com.example.data.model.Customer
import com.example.data.model.DebtPayment
import com.example.data.model.DetailedDebt
import com.example.data.model.Expense
import com.example.data.model.InviteCode
import com.example.data.model.Product
import com.example.data.model.Provision
import com.example.data.model.Requisition
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.StaffMember
import com.example.data.model.StockLoss
import com.example.data.model.StockTransfer
import com.example.data.model.Store
import com.example.data.model.UserSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.random.Random

class KasaRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("L'utilisateur doit être connecté avec Google pour accéder à la base de données.")
    }

    // --- Stores (Succursales / Magasins) ---

    fun observeStores(): Flow<List<Store>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/stores"
        emitAll(
            db.collection("users").document(uid).collection("stores")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Store::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createStore(name: String, address: String = ""): Result<String> {
        val uid = requireUserId()
        val storesRef = db.collection("users").document(uid).collection("stores")
        val docId = UUID.randomUUID().toString()
        val payload = mapOf(
            "name" to name.trim(),
            "address" to address.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            storesRef.document(docId).set(payload).await()
            Result.success(docId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, storesRef.path)
            Result.failure(e)
        }
    }

    // --- Staff & Invite Codes (Gestion des Employés & Inscription) ---

    fun observeStaff(): Flow<List<StaffMember>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/staff"
        emitAll(
            db.collection("users").document(uid).collection("staff")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(StaffMember::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeInviteCodes(): Flow<List<InviteCode>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/invite_codes"
        emitAll(
            db.collection("users").document(uid).collection("invite_codes")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(InviteCode::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun generateInviteCode(storeId: String, storeName: String, role: String = "CASHIER"): Result<String> {
        val uid = requireUserId()
        val codeRef = db.collection("users").document(uid).collection("invite_codes")
        val randomNum = Random.nextInt(1000, 9999)
        val codeString = "KASA-$randomNum"
        val docId = UUID.randomUUID().toString()

        val payload = mapOf(
            "code" to codeString,
            "storeId" to storeId,
            "storeName" to storeName,
            "role" to role,
            "isUsed" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            codeRef.document(docId).set(payload).await()
            Result.success(codeString)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, codeRef.path)
            Result.failure(e)
        }
    }

    suspend fun registerStaffWithInviteCode(
        inputCode: String,
        fullName: String,
        phone: String,
        pinCode: String
    ): Result<StaffMember> {
        val uid = requireUserId()
        val codeQuery = db.collection("users").document(uid).collection("invite_codes")
            .whereEqualTo("code", inputCode.trim().uppercase())
            .whereEqualTo("isUsed", false)
            .get()
            .await()

        if (codeQuery.isEmpty) {
            return Result.failure(IllegalArgumentException("Code d'invitation invalide ou déjà utilisé."))
        }

        val codeDoc = codeQuery.documents.first()
        val inviteCode = codeDoc.toObject(InviteCode::class.java)
            ?: return Result.failure(IllegalStateException("Erreur lors de la lecture du code."))

        val staffRef = db.collection("users").document(uid).collection("staff")
        val staffId = UUID.randomUUID().toString()
        val newStaff = StaffMember(
            id = staffId,
            name = fullName.trim(),
            phone = phone.trim(),
            pinCode = pinCode.trim(),
            storeId = inviteCode.storeId,
            storeName = inviteCode.storeName,
            role = inviteCode.role,
            isActive = true
        )

        val batch = db.batch()
        batch.set(
            staffRef.document(staffId),
            mapOf(
                "name" to newStaff.name,
                "phone" to newStaff.phone,
                "pinCode" to newStaff.pinCode,
                "storeId" to newStaff.storeId,
                "storeName" to newStaff.storeName,
                "role" to newStaff.role,
                "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(codeDoc.reference, mapOf("isUsed" to true, "usedByStaffName" to fullName.trim()))

        return try {
            batch.commit().await()
            Result.success(newStaff)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, staffRef.path)
            Result.failure(e)
        }
    }

    // --- Products (Stock) ---

    fun observeProducts(): Flow<List<Product>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/products"
        emitAll(
            db.collection("users").document(uid).collection("products")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Product::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveProduct(product: Product): Result<String> {
        val uid = requireUserId()
        val productsRef = db.collection("users").document(uid).collection("products")
        val docId = if (product.id.isNotBlank()) product.id else UUID.randomUUID().toString()
        val docRef = productsRef.document(docId)

        val payload = mutableMapOf<String, Any>(
            "name" to product.name.trim(),
            "category" to product.category.trim(),
            "barcode" to product.barcode.trim(),
            "storeId" to product.storeId,
            "storeName" to product.storeName,
            "purchasePriceUSD" to product.purchasePriceUSD,
            "sellingPriceUSD" to product.sellingPriceUSD,
            "stockQuantity" to product.stockQuantity,
            "minStockAlert" to product.minStockAlert,
            "unit" to product.unit.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (product.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        return try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(docId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adjustProductStock(productId: String, quantityChange: Double): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("products").document(productId)
        return try {
            docRef.update(
                mapOf(
                    "stockQuantity" to FieldValue.increment(quantityChange),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("products").document(productId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Transfert de Stock Inter-Magasins ---

    suspend fun transferStock(
        product: Product,
        fromStoreId: String,
        fromStoreName: String,
        toStoreId: String,
        toStoreName: String,
        quantity: Double,
        staffName: String
    ): Result<Unit> {
        val uid = requireUserId()
        val transferRef = db.collection("users").document(uid).collection("stock_transfers").document()

        // 1. Decrement source product
        val sourceRef = db.collection("users").document(uid).collection("products").document(product.id)

        // 2. Find or create matching product in destination store
        val destQuery = db.collection("users").document(uid).collection("products")
            .whereEqualTo("storeId", toStoreId)
            .whereEqualTo("name", product.name)
            .get()
            .await()

        val batch = db.batch()

        // Log Transfer record
        val transferPayload = mapOf(
            "fromStoreId" to fromStoreId,
            "fromStoreName" to fromStoreName,
            "toStoreId" to toStoreId,
            "toStoreName" to toStoreName,
            "productId" to product.id,
            "productName" to product.name,
            "quantity" to quantity,
            "unit" to product.unit,
            "staffName" to staffName,
            "createdAt" to FieldValue.serverTimestamp()
        )
        batch.set(transferRef, transferPayload)

        // Decrement source
        batch.update(
            sourceRef,
            mapOf(
                "stockQuantity" to FieldValue.increment(-quantity),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        // Increment or create destination
        if (!destQuery.isEmpty) {
            val destDoc = destQuery.documents.first()
            batch.update(
                destDoc.reference,
                mapOf(
                    "stockQuantity" to FieldValue.increment(quantity),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            val newDestRef = db.collection("users").document(uid).collection("products").document(UUID.randomUUID().toString())
            val newDestPayload = mapOf(
                "name" to product.name,
                "category" to product.category,
                "barcode" to product.barcode,
                "storeId" to toStoreId,
                "storeName" to toStoreName,
                "purchasePriceUSD" to product.purchasePriceUSD,
                "sellingPriceUSD" to product.sellingPriceUSD,
                "stockQuantity" to quantity,
                "minStockAlert" to product.minStockAlert,
                "unit" to product.unit,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(newDestRef, newDestPayload)
        }

        return try {
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, transferRef.path)
            Result.failure(e)
        }
    }

    // --- Pertes & Avaries ---

    suspend fun recordStockLoss(
        product: Product,
        quantity: Double,
        reason: String,
        staffName: String
    ): Result<Unit> {
        val uid = requireUserId()
        val lossRef = db.collection("users").document(uid).collection("stock_losses").document()
        val productRef = db.collection("users").document(uid).collection("products").document(product.id)

        val batch = db.batch()
        batch.set(
            lossRef,
            mapOf(
                "storeId" to product.storeId,
                "storeName" to product.storeName,
                "productId" to product.id,
                "productName" to product.name,
                "quantity" to quantity,
                "unit" to product.unit,
                "reason" to reason,
                "estimatedLossUSD" to (product.purchasePriceUSD * quantity),
                "staffName" to staffName,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            productRef,
            mapOf(
                "stockQuantity" to FieldValue.increment(-quantity),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        return try {
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, lossRef.path)
            Result.failure(e)
        }
    }

    // --- Clôture de Caisse Quotidienne ---

    fun observeCashClosings(): Flow<List<CashClosing>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/cash_closings"
        emitAll(
            db.collection("users").document(uid).collection("cash_closings")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(CashClosing::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun recordCashClosing(closing: CashClosing): Result<String> {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("cash_closings").document()
        val payload = mapOf(
            "storeId" to closing.storeId,
            "storeName" to closing.storeName,
            "staffName" to closing.staffName,
            "systemSalesUSD" to closing.systemSalesUSD,
            "systemSalesFC" to closing.systemSalesFC,
            "countedCashUSD" to closing.countedCashUSD,
            "countedCashFC" to closing.countedCashFC,
            "discrepancyUSD" to closing.discrepancyUSD,
            "discrepancyFC" to closing.discrepancyFC,
            "notes" to closing.notes.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    // --- Dettes Détaillées par Article & Acomptes ---

    fun observeDetailedDebts(): Flow<List<DetailedDebt>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/detailed_debts"
        emitAll(
            db.collection("users").document(uid).collection("detailed_debts")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(DetailedDebt::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveDetailedDebt(debt: DetailedDebt): Result<String> {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("detailed_debts")
        val docId = if (debt.id.isNotBlank()) debt.id else UUID.randomUUID().toString()
        val docRef = ref.document(docId)

        val status = when {
            debt.remainingUSD <= 0.01 -> "PAID"
            debt.advancePaidUSD > 0.01 -> "PARTIAL_ADVANCE"
            else -> "UNPAID"
        }

        val payload = mapOf(
            "storeId" to debt.storeId,
            "storeName" to debt.storeName,
            "clientName" to debt.clientName.trim(),
            "clientPhone" to debt.clientPhone.trim(),
            "productName" to debt.productName.trim(),
            "quantity" to debt.quantity,
            "unit" to debt.unit.trim(),
            "totalAmountUSD" to debt.totalAmountUSD,
            "totalAmountFC" to debt.totalAmountFC,
            "advancePaidUSD" to debt.advancePaidUSD,
            "advancePaidFC" to debt.advancePaidFC,
            "dueDate" to debt.dueDate,
            "status" to status,
            "staffName" to debt.staffName,
            "notes" to debt.notes.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(docId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun payDetailedDebt(
        debtId: String,
        paymentUSD: Double,
        paymentFC: Double,
        notes: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("detailed_debts").document(debtId)
        val snapshot = docRef.get().await()
        val currentDebt = snapshot.toObject(DetailedDebt::class.java)
            ?: return Result.failure(IllegalStateException("Dette introuvable"))

        val newAdvanceUSD = currentDebt.advancePaidUSD + paymentUSD
        val newAdvanceFC = currentDebt.advancePaidFC + paymentFC
        val newRemaining = currentDebt.totalAmountUSD - newAdvanceUSD

        val newStatus = if (newRemaining <= 0.01) "PAID" else "PARTIAL_ADVANCE"

        return try {
            docRef.update(
                mapOf(
                    "advancePaidUSD" to newAdvanceUSD,
                    "advancePaidFC" to newAdvanceFC,
                    "status" to newStatus,
                    "notes" to (currentDebt.notes + " | Acompte: $paymentUSD$ ($notes)").trim()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Provisions (Commandes Payées d'Avance) ---

    fun observeProvisions(): Flow<List<Provision>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/provisions"
        emitAll(
            db.collection("users").document(uid).collection("provisions")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Provision::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveProvision(provision: Provision): Result<String> {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("provisions")
        val docId = if (provision.id.isNotBlank()) provision.id else UUID.randomUUID().toString()
        val docRef = ref.document(docId)

        val payload = mapOf(
            "storeId" to provision.storeId,
            "storeName" to provision.storeName,
            "clientName" to provision.clientName.trim(),
            "clientPhone" to provision.clientPhone.trim(),
            "productName" to provision.productName.trim(),
            "quantity" to provision.quantity,
            "unit" to provision.unit.trim(),
            "advanceAmountUSD" to provision.advanceAmountUSD,
            "advanceAmountFC" to provision.advanceAmountFC,
            "totalPriceUSD" to provision.totalPriceUSD,
            "dueDate" to provision.dueDate,
            "status" to provision.status,
            "staffName" to provision.staffName,
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(docId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun convertProvisionToSale(
        provision: Provision,
        exchangeRate: Double,
        profitUSD: Double = 0.0
    ): Result<Unit> {
        val uid = requireUserId()
        val provisionRef = db.collection("users").document(uid).collection("provisions").document(provision.id)
        val salesRef = db.collection("users").document(uid).collection("sales").document()

        val batch = db.batch()

        // 1. Mark provision as DELIVERED
        batch.update(provisionRef, mapOf("status" to "DELIVERED"))

        // 2. Create Sale document
        val saleItem = mapOf(
            "productId" to "",
            "productName" to provision.productName,
            "quantity" to provision.quantity,
            "unitPriceUSD" to (provision.totalPriceUSD / provision.quantity),
            "purchasePriceUSD" to 0.0,
            "totalUSD" to provision.totalPriceUSD
        )

        val salePayload = mapOf(
            "storeId" to provision.storeId,
            "storeName" to provision.storeName,
            "staffName" to provision.staffName,
            "items" to listOf(saleItem),
            "totalUSD" to provision.totalPriceUSD,
            "totalFC" to (provision.totalPriceUSD * exchangeRate),
            "currencyUsed" to "USD",
            "exchangeRate" to exchangeRate,
            "grossProfitUSD" to profitUSD,
            "customerId" to "",
            "customerName" to provision.clientName,
            "customerPhone" to provision.clientPhone,
            "paymentType" to "CASH",
            "isPaid" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )
        batch.set(salesRef, salePayload)

        return try {
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, provisionRef.path)
            Result.failure(e)
        }
    }

    // --- Réquisitions (Besoins Clients Non Satisfaits avec Vote +1) ---

    fun observeRequisitions(): Flow<List<Requisition>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/requisitions"
        emitAll(
            db.collection("users").document(uid).collection("requisitions")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Requisition::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveRequisition(requisition: Requisition): Result<String> {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("requisitions")
        val docId = if (requisition.id.isNotBlank()) requisition.id else UUID.randomUUID().toString()
        val docRef = ref.document(docId)

        val payload = mapOf(
            "storeId" to requisition.storeId,
            "storeName" to requisition.storeName,
            "productName" to requisition.productName.trim(),
            "category" to requisition.category.trim(),
            "requestCount" to requisition.requestCount,
            "notes" to requisition.notes.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(docId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun incrementRequisitionCount(requisitionId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("requisitions").document(requisitionId)
        return try {
            docRef.update(mapOf("requestCount" to FieldValue.increment(1))).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Sales (Ventes) ---

    fun observeSales(): Flow<List<Sale>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/sales"
        emitAll(
            db.collection("users").document(uid).collection("sales")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Sale::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun recordSale(
        sale: Sale,
        cartItems: List<SaleItem>
    ): Result<String> {
        val uid = requireUserId()
        val salesRef = db.collection("users").document(uid).collection("sales")
        val saleId = if (sale.id.isNotBlank()) sale.id else UUID.randomUUID().toString()
        val saleDocRef = salesRef.document(saleId)

        val batch = db.batch()

        val rawItems = cartItems.map { item ->
            mapOf(
                "productId" to item.productId,
                "productName" to item.productName,
                "quantity" to item.quantity,
                "unitPriceUSD" to item.unitPriceUSD,
                "purchasePriceUSD" to item.purchasePriceUSD,
                "totalUSD" to item.totalUSD
            )
        }

        val salePayload = mutableMapOf<String, Any>(
            "storeId" to sale.storeId,
            "storeName" to sale.storeName,
            "staffName" to sale.staffName,
            "items" to rawItems,
            "totalUSD" to sale.totalUSD,
            "totalFC" to sale.totalFC,
            "currencyUsed" to sale.currencyUsed,
            "exchangeRate" to sale.exchangeRate,
            "grossProfitUSD" to sale.grossProfitUSD,
            "customerId" to sale.customerId,
            "customerName" to sale.customerName,
            "customerPhone" to sale.customerPhone,
            "paymentType" to sale.paymentType,
            "isPaid" to sale.isPaid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        batch.set(saleDocRef, salePayload)

        // Decrement stock
        for (item in cartItems) {
            if (item.productId.isNotBlank()) {
                val productRef = db.collection("users").document(uid).collection("products").document(item.productId)
                batch.update(
                    productRef,
                    mapOf(
                        "stockQuantity" to FieldValue.increment(-item.quantity),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }

        return try {
            batch.commit().await()
            Result.success(saleId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, saleDocRef.path)
            Result.failure(e)
        }
    }

    // --- Expenses (Dépenses) ---

    fun observeExpenses(): Flow<List<Expense>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/expenses"
        emitAll(
            db.collection("users").document(uid).collection("expenses")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Expense::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveExpense(expense: Expense): Result<String> {
        val uid = requireUserId()
        val expensesRef = db.collection("users").document(uid).collection("expenses")
        val docId = if (expense.id.isNotBlank()) expense.id else UUID.randomUUID().toString()
        val docRef = expensesRef.document(docId)

        val payload = mutableMapOf<String, Any>(
            "title" to expense.title.trim(),
            "category" to expense.category.trim(),
            "amountUSD" to expense.amountUSD,
            "amountFC" to expense.amountFC,
            "currencyUsed" to expense.currencyUsed,
            "notes" to expense.notes.trim()
        )
        if (expense.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        return try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(docId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("expenses").document(expenseId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Settings (Devises & Boutique) ---

    fun observeSettings(): Flow<UserSettings> = flow {
        val uid = requireUserId()
        val path = "users/$uid/settings/config"
        emitAll(
            db.collection("users").document(uid).collection("settings").document("config")
                .snapshots()
                .map { snapshot -> snapshot.toObject(UserSettings::class.java) ?: UserSettings() }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    emit(UserSettings())
                }
        )
    }

    suspend fun updateSettings(settings: UserSettings): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("settings").document("config")
        val payload = mapOf(
            "shopName" to settings.shopName.trim(),
            "exchangeRate" to settings.exchangeRate,
            "primaryCurrency" to settings.primaryCurrency,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // Seed default stores and starter items if newly created
    suspend fun seedInitialDataIfEmpty(currentStoreCount: Int) {
        if (currentStoreCount > 0) return
        val store1Id = UUID.randomUUID().toString()
        val store2Id = UUID.randomUUID().toString()

        val store1 = Store(id = store1Id, name = "Boutique Centre-Ville", address = "Avenue Principale")
        val store2 = Store(id = store2Id, name = "Dépôt & Succursale 2", address = "Marché Central")

        createStore(store1.name, store1.address)
        createStore(store2.name, store2.address)

        val starterProducts = listOf(
            Product(storeId = store1Id, storeName = store1.name, name = "Riz Blanc 25kg (Bongo)", category = "Alimentation", purchasePriceUSD = 22.0, sellingPriceUSD = 26.0, stockQuantity = 25.0, minStockAlert = 5.0, unit = "sac"),
            Product(storeId = store1Id, storeName = store1.name, name = "Huile Végétale 5L (Mayor)", category = "Alimentation", purchasePriceUSD = 7.5, sellingPriceUSD = 9.0, stockQuantity = 30.0, minStockAlert = 6.0, unit = "bidon"),
            Product(storeId = store1Id, storeName = store1.name, name = "Sucre Blanc 50kg", category = "Alimentation", purchasePriceUSD = 45.0, sellingPriceUSD = 52.0, stockQuantity = 12.0, minStockAlert = 3.0, unit = "sac"),
            Product(storeId = store2Id, storeName = store2.name, name = "Savon Le Coq (Carton 24)", category = "Entretien", purchasePriceUSD = 11.0, sellingPriceUSD = 13.5, stockQuantity = 4.0, minStockAlert = 5.0, unit = "carton"),
            Product(storeId = store2Id, storeName = store2.name, name = "Lait Bonnet Rouge (Pack)", category = "Alimentation", purchasePriceUSD = 14.0, sellingPriceUSD = 16.5, stockQuantity = 1.0, minStockAlert = 4.0, unit = "pack")
        )
        for (p in starterProducts) {
            saveProduct(p)
        }
    }
}
