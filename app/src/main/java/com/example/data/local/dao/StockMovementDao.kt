package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity)

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC LIMIT 50")
    fun getMovementsForProduct(productId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC LIMIT 100")
    fun getAllRecentMovements(): Flow<List<StockMovementEntity>>

    @Query("DELETE FROM stock_movements WHERE productId = :productId")
    suspend fun deleteMovementsForProduct(productId: String)
}
