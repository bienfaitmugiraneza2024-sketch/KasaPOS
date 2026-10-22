package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "config",
    val shopName: String = "Mon Commerce",
    val exchangeRate: Double = 2850.0, // 1 USD = 2850 FC
    val primaryCurrency: String = "USD", // "USD" or "FC"
    @ServerTimestamp val updatedAt: Timestamp? = null
)
