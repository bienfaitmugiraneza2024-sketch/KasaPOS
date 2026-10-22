package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val name: String = "",
    val category: String = "Général",
    val barcode: String = "",
    val purchasePriceUSD: Double = 0.0,
    val sellingPriceUSD: Double = 0.0,
    val stockQuantity: Double = 0.0,
    val minStockAlert: Double = 5.0,
    val unit: String = "pcs",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isOutOfStock: Boolean
        get() = stockQuantity <= 0.0

    @get:Exclude
    val isLowStock: Boolean
        get() = stockQuantity in 0.001..minStockAlert

    @get:Exclude
    val marginUSD: Double
        get() = sellingPriceUSD - purchasePriceUSD

    @get:Exclude
    val marginPercent: Double
        get() = if (purchasePriceUSD > 0) (marginUSD / purchasePriceUSD) * 100.0 else 0.0
}
