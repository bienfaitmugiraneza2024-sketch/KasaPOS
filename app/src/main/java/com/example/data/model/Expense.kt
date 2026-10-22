package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "Divers",
    val amountUSD: Double = 0.0,
    val amountFC: Double = 0.0,
    val currencyUsed: String = "USD",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
