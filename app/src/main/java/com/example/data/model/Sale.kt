package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class SaleItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Double = 1.0,
    val unitPriceUSD: Double = 0.0,
    val purchasePriceUSD: Double = 0.0,
    val totalUSD: Double = 0.0
) {
    @get:Exclude
    val profitUSD: Double
        get() = (unitPriceUSD - purchasePriceUSD) * quantity
}

@IgnoreExtraProperties
data class Sale(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val staffName: String = "Gérant",
    val items: List<Map<String, Any>> = emptyList(),
    val totalUSD: Double = 0.0,
    val totalFC: Double = 0.0,
    val currencyUsed: String = "USD", // "USD" or "FC"
    val exchangeRate: Double = 2850.0,
    val grossProfitUSD: Double = 0.0,
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val paymentType: String = "CASH", // "CASH" or "DEBT"
    val isPaid: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)
