package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Store(
    @DocumentId val id: String = "",
    val name: String = "",
    val address: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InviteCode(
    @DocumentId val id: String = "",
    val code: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val role: String = "CASHIER",
    val isUsed: Boolean = false,
    val usedByStaffName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StaffMember(
    @DocumentId val id: String = "",
    val name: String = "",
    val phone: String = "",
    val pinCode: String = "0000",
    val storeId: String = "",
    val storeName: String = "",
    val role: String = "CASHIER", // "ADMIN" or "CASHIER"
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:Exclude
    val isAdmin: Boolean
        get() = role.equals("ADMIN", ignoreCase = true)
}
