package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Product
import java.util.UUID

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val storeId: String = "",
    val storeName: String = "Boutique Principale",
    val name: String,
    val category: String = "Général",
    val barcode: String = "",
    val purchasePriceUSD: Double = 0.0,
    val sellingPriceUSD: Double = 0.0,
    val stockQuantity: Double = 0.0,
    val minStockAlert: Double = 5.0,
    val unit: String = "pcs",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isOutOfStock: Boolean
        get() = stockQuantity <= 0.0

    val isLowStock: Boolean
        get() = stockQuantity > 0.0 && stockQuantity <= minStockAlert

    val marginUSD: Double
        get() = sellingPriceUSD - purchasePriceUSD

    val marginPercent: Double
        get() = if (purchasePriceUSD > 0) (marginUSD / purchasePriceUSD) * 100.0 else 0.0

    fun toProduct(): Product {
        return Product(
            id = id,
            storeId = storeId,
            storeName = storeName,
            name = name,
            category = category,
            barcode = barcode,
            purchasePriceUSD = purchasePriceUSD,
            sellingPriceUSD = sellingPriceUSD,
            stockQuantity = stockQuantity,
            minStockAlert = minStockAlert,
            unit = unit
        )
    }
}

fun Product.toEntity(description: String = ""): ProductEntity {
    return ProductEntity(
        id = if (id.isNotBlank()) id else UUID.randomUUID().toString(),
        storeId = storeId,
        storeName = storeName,
        name = name,
        category = category.ifBlank { "Général" },
        barcode = barcode,
        purchasePriceUSD = purchasePriceUSD,
        sellingPriceUSD = sellingPriceUSD,
        stockQuantity = stockQuantity,
        minStockAlert = if (minStockAlert > 0) minStockAlert else 5.0,
        unit = unit.ifBlank { "pcs" },
        description = description,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
        updatedAt = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}
