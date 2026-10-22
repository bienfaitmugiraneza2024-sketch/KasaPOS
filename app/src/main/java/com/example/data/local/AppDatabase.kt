package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.StockMovementDao
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ProductEntity::class, StockMovementEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun stockMovementDao(): StockMovementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasa_stock_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate starter catalog on creation
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    seedDefaultCatalog(database)
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDefaultCatalog(database: AppDatabase) {
            val now = System.currentTimeMillis()
            val initialProducts = listOf(
                ProductEntity(
                    id = "seed-prod-1",
                    name = "Sucre Blanc Fin 50kg",
                    category = "Alimentation",
                    barcode = "SUC-50KG-001",
                    purchasePriceUSD = 40.0,
                    sellingPriceUSD = 48.0,
                    stockQuantity = 22.0,
                    minStockAlert = 5.0,
                    unit = "sac",
                    description = "Sac de sucre blanc raffiné qualité supérieure",
                    createdAt = now,
                    updatedAt = now
                ),
                ProductEntity(
                    id = "seed-prod-2",
                    name = "Huile Végétale Simba 5L",
                    category = "Alimentation",
                    barcode = "HUI-5L-002",
                    purchasePriceUSD = 9.5,
                    sellingPriceUSD = 12.0,
                    stockQuantity = 4.0, // Low stock alert! (<= 5.0)
                    minStockAlert = 6.0,
                    unit = "bidon",
                    description = "Bidon d'huile raffinée sans cholestérol",
                    createdAt = now,
                    updatedAt = now
                ),
                ProductEntity(
                    id = "seed-prod-3",
                    name = "Riz Blanc Boma 25kg",
                    category = "Alimentation",
                    barcode = "RIZ-25K-003",
                    purchasePriceUSD = 21.0,
                    sellingPriceUSD = 26.5,
                    stockQuantity = 0.0, // Out of stock! (Rupture)
                    minStockAlert = 4.0,
                    unit = "sac",
                    description = "Riz blanc parfumé 25kg",
                    createdAt = now,
                    updatedAt = now
                ),
                ProductEntity(
                    id = "seed-prod-4",
                    name = "Lait en Poudre Nido 400g",
                    category = "Boissons & Laiterie",
                    barcode = "NID-400-004",
                    purchasePriceUSD = 3.8,
                    sellingPriceUSD = 5.0,
                    stockQuantity = 3.0, // Low stock alert!
                    minStockAlert = 8.0,
                    unit = "boîte",
                    description = "Boîte de lait enrichi en fer et vitamines",
                    createdAt = now,
                    updatedAt = now
                ),
                ProductEntity(
                    id = "seed-prod-5",
                    name = "Savon Brillant Multiusage",
                    category = "Entretien & Hygiène",
                    barcode = "SAV-MUL-005",
                    purchasePriceUSD = 0.40,
                    sellingPriceUSD = 0.70,
                    stockQuantity = 85.0,
                    minStockAlert = 15.0,
                    unit = "pcs",
                    description = "Barre de savon détachant et nettoyant ménager",
                    createdAt = now,
                    updatedAt = now
                ),
                ProductEntity(
                    id = "seed-prod-6",
                    name = "Farine de Froment Azam 25kg",
                    category = "Alimentation",
                    barcode = "FAR-25K-006",
                    purchasePriceUSD = 18.0,
                    sellingPriceUSD = 22.0,
                    stockQuantity = 12.0,
                    minStockAlert = 3.0,
                    unit = "sac",
                    description = "Farine boulangère de haute qualité",
                    createdAt = now,
                    updatedAt = now
                ),
                ProductEntity(
                    id = "seed-prod-7",
                    name = "Eau Minérale Swissta 1.5L (Pack x6)",
                    category = "Boissons & Laiterie",
                    barcode = "SWI-15L-007",
                    purchasePriceUSD = 2.5,
                    sellingPriceUSD = 3.5,
                    stockQuantity = 2.0, // Low stock alert!
                    minStockAlert = 5.0,
                    unit = "pack",
                    description = "Pack de 6 bouteilles d'eau minérale naturelle",
                    createdAt = now,
                    updatedAt = now
                )
            )

            database.productDao().insertProducts(initialProducts)

            // Seed initial stock movements
            for (p in initialProducts) {
                database.stockMovementDao().insertMovement(
                    StockMovementEntity(
                        productId = p.id,
                        productName = p.name,
                        storeId = p.storeId,
                        movementType = "INITIAL",
                        quantityChange = p.stockQuantity,
                        previousStock = 0.0,
                        newStock = p.stockQuantity,
                        reason = "Inventaire initial du catalogue",
                        operatorName = "Gérant Système",
                        timestamp = now
                    )
                )
            }
        }
    }
}
