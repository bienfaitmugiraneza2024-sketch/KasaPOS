package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: String,
    val productName: String,
    val storeId: String = "",
    val movementType: String, // "ENTRY", "SALE", "LOSS", "TRANSFER", "ADJUSTMENT", "INITIAL"
    val quantityChange: Double,
    val previousStock: Double,
    val newStock: Double,
    val reason: String = "",
    val operatorName: String = "Gérant",
    val timestamp: Long = System.currentTimeMillis()
)
