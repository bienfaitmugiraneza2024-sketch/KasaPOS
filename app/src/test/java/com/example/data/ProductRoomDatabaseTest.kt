package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ProductRoomRepository
import com.example.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProductRoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ProductRoomRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ProductRoomRepository(db.productDao(), db.stockMovementDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testProductInsertAndQuery() = runBlocking {
        val product = ProductEntity(
            id = "test-prod-1",
            name = "Riz Blanc 25kg",
            category = "Alimentation",
            purchasePriceUSD = 20.0,
            sellingPriceUSD = 25.0,
            stockQuantity = 10.0,
            minStockAlert = 5.0
        )

        repository.saveProduct(product, "Tester")

        val list = repository.allProducts.first()
        assertEquals(1, list.size)
        assertEquals("Riz Blanc 25kg", list.first().name)
        assertEquals(5.0, list.first().marginUSD, 0.001)
    }

    @Test
    fun testLowStockAndOutOfStockAlerts() = runBlocking {
        val normalProd = ProductEntity(
            id = "p-1",
            name = "Savon",
            stockQuantity = 50.0,
            minStockAlert = 10.0
        )
        val lowStockProd = ProductEntity(
            id = "p-2",
            name = "Huile 5L",
            stockQuantity = 3.0,
            minStockAlert = 5.0
        )
        val outOfStockProd = ProductEntity(
            id = "p-3",
            name = "Farine",
            stockQuantity = 0.0,
            minStockAlert = 4.0
        )

        repository.saveProduct(normalProd)
        repository.saveProduct(lowStockProd)
        repository.saveProduct(outOfStockProd)

        val lowStockList = repository.lowStockProducts.first()
        assertEquals(1, lowStockList.size)
        assertEquals("Huile 5L", lowStockList.first().name)
        assertTrue(lowStockList.first().isLowStock)

        val outOfStockList = repository.outOfStockProducts.first()
        assertEquals(1, outOfStockList.size)
        assertEquals("Farine", outOfStockList.first().name)
        assertTrue(outOfStockList.first().isOutOfStock)
    }

    @Test
    fun testStockAdjustmentAndMovementLogging() = runBlocking {
        val product = ProductEntity(
            id = "p-audit",
            name = "Lait Nido",
            stockQuantity = 10.0,
            minStockAlert = 3.0
        )
        repository.saveProduct(product, "Gérant")

        // Quick restock +5
        repository.adjustStock("p-audit", 5.0, "Livraison fournisseur", "Gérant", "ENTRY")

        val updated = repository.getProductById("p-audit")
        assertNotNull(updated)
        assertEquals(15.0, updated!!.stockQuantity, 0.001)

        val movements = repository.getMovementsForProduct("p-audit").first()
        // Should have INITIAL and ENTRY movements
        assertEquals(2, movements.size)
        assertEquals(5.0, movements.first().quantityChange, 0.001)
        assertEquals(15.0, movements.first().newStock, 0.001)
    }

    @Test
    fun testProductDelete() = runBlocking {
        val product = ProductEntity(
            id = "p-del",
            name = "Article à supprimer",
            stockQuantity = 0.0
        )
        repository.saveProduct(product)
        assertEquals(1, repository.allProducts.first().size)

        repository.deleteProduct("p-del")
        assertEquals(0, repository.allProducts.first().size)
        assertNull(repository.getProductById("p-del"))
    }
}
