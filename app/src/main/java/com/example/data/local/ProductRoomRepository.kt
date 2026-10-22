package com.example.data.local

import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.StockMovementDao
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.toEntity
import com.example.data.model.Product
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ProductRoomRepository(
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao
) {

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    val lowStockProducts: Flow<List<ProductEntity>> = productDao.getLowStockProducts()

    val outOfStockProducts: Flow<List<ProductEntity>> = productDao.getOutOfStockProducts()

    val allCategories: Flow<List<String>> = productDao.getAllCategories()

    val recentMovements: Flow<List<StockMovementEntity>> = stockMovementDao.getAllRecentMovements()

    fun getMovementsForProduct(productId: String): Flow<List<StockMovementEntity>> {
        return stockMovementDao.getMovementsForProduct(productId)
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return productDao.searchProducts(query)
    }

    suspend fun getProductById(id: String): ProductEntity? {
        return productDao.getProductById(id)
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return productDao.getProductByBarcode(barcode)
    }

    suspend fun saveProduct(product: ProductEntity, operator: String = "Gérant"): String {
        val now = System.currentTimeMillis()
        val finalId = if (product.id.isNotBlank()) product.id else UUID.randomUUID().toString()
        val existing = productDao.getProductById(finalId)

        val updatedProduct = product.copy(
            id = finalId,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )

        productDao.insertProduct(updatedProduct)

        if (existing == null) {
            // New product created
            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = finalId,
                    productName = updatedProduct.name,
                    storeId = updatedProduct.storeId,
                    movementType = "INITIAL",
                    quantityChange = updatedProduct.stockQuantity,
                    previousStock = 0.0,
                    newStock = updatedProduct.stockQuantity,
                    reason = "Création de la fiche produit",
                    operatorName = operator,
                    timestamp = now
                )
            )
        } else if (existing.stockQuantity != updatedProduct.stockQuantity) {
            // Stock was updated during edit
            val diff = updatedProduct.stockQuantity - existing.stockQuantity
            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = finalId,
                    productName = updatedProduct.name,
                    storeId = updatedProduct.storeId,
                    movementType = if (diff > 0) "ENTRY" else "ADJUSTMENT",
                    quantityChange = diff,
                    previousStock = existing.stockQuantity,
                    newStock = updatedProduct.stockQuantity,
                    reason = "Modification manuelle du stock lors de l'édition",
                    operatorName = operator,
                    timestamp = now
                )
            )
        }

        return finalId
    }

    suspend fun deleteProduct(id: String) {
        stockMovementDao.deleteMovementsForProduct(id)
        productDao.deleteProductById(id)
    }

    suspend fun adjustStock(
        productId: String,
        delta: Double,
        reason: String,
        operator: String = "Gérant",
        movementType: String = if (delta >= 0) "ENTRY" else "ADJUSTMENT"
    ) {
        val existing = productDao.getProductById(productId) ?: return
        val now = System.currentTimeMillis()
        val newStock = (existing.stockQuantity + delta).coerceAtLeast(0.0)

        productDao.setExactStock(productId, newStock, now)

        stockMovementDao.insertMovement(
            StockMovementEntity(
                productId = productId,
                productName = existing.name,
                storeId = existing.storeId,
                movementType = movementType,
                quantityChange = delta,
                previousStock = existing.stockQuantity,
                newStock = newStock,
                reason = reason.ifBlank { if (delta >= 0) "Réapprovisionnement" else "Ajustement stock" },
                operatorName = operator,
                timestamp = now
            )
        )
    }

    suspend fun setExactStock(
        productId: String,
        exactStock: Double,
        reason: String,
        operator: String = "Gérant"
    ) {
        val existing = productDao.getProductById(productId) ?: return
        val now = System.currentTimeMillis()
        val diff = exactStock - existing.stockQuantity

        productDao.setExactStock(productId, exactStock.coerceAtLeast(0.0), now)

        stockMovementDao.insertMovement(
            StockMovementEntity(
                productId = productId,
                productName = existing.name,
                storeId = existing.storeId,
                movementType = "ADJUSTMENT",
                quantityChange = diff,
                previousStock = existing.stockQuantity,
                newStock = exactStock.coerceAtLeast(0.0),
                reason = reason.ifBlank { "Inventaire physique périodique" },
                operatorName = operator,
                timestamp = now
            )
        )
    }

    suspend fun recordLoss(
        productId: String,
        lossQty: Double,
        reason: String,
        operator: String = "Gérant"
    ) {
        val existing = productDao.getProductById(productId) ?: return
        val now = System.currentTimeMillis()
        val newStock = (existing.stockQuantity - lossQty).coerceAtLeast(0.0)

        productDao.setExactStock(productId, newStock, now)

        stockMovementDao.insertMovement(
            StockMovementEntity(
                productId = productId,
                productName = existing.name,
                storeId = existing.storeId,
                movementType = "LOSS",
                quantityChange = -lossQty,
                previousStock = existing.stockQuantity,
                newStock = newStock,
                reason = reason.ifBlank { "Déclaration de perte / avarie" },
                operatorName = operator,
                timestamp = now
            )
        )
    }

    suspend fun syncCloudProductsToRoom(cloudProducts: List<Product>) {
        if (cloudProducts.isEmpty()) return
        val entities = cloudProducts.map { it.toEntity() }
        productDao.insertProducts(entities)
    }
}
