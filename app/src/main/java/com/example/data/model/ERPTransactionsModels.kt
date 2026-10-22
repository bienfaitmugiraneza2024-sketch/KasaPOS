package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CashClosing(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val staffName: String = "",
    val systemSalesUSD: Double = 0.0,
    val systemSalesFC: Double = 0.0,
    val countedCashUSD: Double = 0.0,
    val countedCashFC: Double = 0.0,
    val discrepancyUSD: Double = 0.0,
    val discrepancyFC: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StockTransfer(
    @DocumentId val id: String = "",
    val fromStoreId: String = "",
    val fromStoreName: String = "",
    val toStoreId: String = "",
    val toStoreName: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "pcs",
    val staffName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StockLoss(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "pcs",
    val reason: String = "Casse", // Casse, Péremption, Vol, Défaut, etc.
    val estimatedLossUSD: Double = 0.0,
    val staffName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DetailedDebt(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val productName: String = "",
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val totalAmountUSD: Double = 0.0,
    val totalAmountFC: Double = 0.0,
    val advancePaidUSD: Double = 0.0,
    val advancePaidFC: Double = 0.0,
    val dueDate: String = "",
    val status: String = "UNPAID", // "PAID", "PARTIAL_ADVANCE", "UNPAID"
    val staffName: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:Exclude
    val remainingUSD: Double
        get() = (totalAmountUSD - advancePaidUSD).coerceAtLeast(0.0)

    @get:Exclude
    val remainingFC: Double
        get() = (totalAmountFC - advancePaidFC).coerceAtLeast(0.0)

    @get:Exclude
    val isPaid: Boolean
        get() = remainingUSD <= 0.01

    @get:Exclude
    val isPartial: Boolean
        get() = advancePaidUSD > 0.01 && !isPaid
}

@IgnoreExtraProperties
data class Provision(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val productName: String = "",
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val advanceAmountUSD: Double = 0.0,
    val advanceAmountFC: Double = 0.0,
    val totalPriceUSD: Double = 0.0,
    val dueDate: String = "",
    val status: String = "PENDING", // "PENDING", "DELIVERED", "CANCELLED"
    val staffName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Requisition(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val productName: String = "",
    val category: String = "Général",
    val requestCount: Int = 1,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
