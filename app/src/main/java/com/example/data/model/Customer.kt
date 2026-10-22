package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val fullName: String = "",
    val phone: String = "",
    val totalDebtUSD: Double = 0.0,
    val totalDebtFC: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val hasOutstandingDebt: Boolean
        get() = totalDebtUSD > 0.01 || totalDebtFC > 10.0
}
