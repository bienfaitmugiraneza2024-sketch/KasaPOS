package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE storeId = :storeId OR :storeId = '' ORDER BY name ASC")
    fun getProductsByStore(storeId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stockQuantity > 0.0 AND stockQuantity <= minStockAlert ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stockQuantity <= 0.0 ORDER BY updatedAt DESC")
    fun getOutOfStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE (name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    @Query("UPDATE products SET stockQuantity = stockQuantity + :deltaQty, updatedAt = :updatedAt WHERE id = :id")
    suspend fun adjustStock(id: String, deltaQty: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQuantity = :newStock, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setExactStock(id: String, newStock: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM products")
    fun getProductCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE stockQuantity <= minStockAlert")
    fun getLowStockCount(): Flow<Int>

    @Query("SELECT DISTINCT category FROM products WHERE category != '' ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>
}
