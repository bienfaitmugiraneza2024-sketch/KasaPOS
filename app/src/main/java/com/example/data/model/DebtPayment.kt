package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class DebtPayment(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val amountUSD: Double = 0.0,
    val amountFC: Double = 0.0,
    val exchangeRate: Double = 2850.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
