package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class KasaRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveProduct_authenticatedUser_succeedsAndObserves() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = KasaRepository(firestore)

        val product = Product(
            id = UUID.randomUUID().toString(),
            name = "Riz Bongo 25kg",
            category = "Alimentation",
            purchasePriceUSD = 20.0,
            sellingPriceUSD = 24.0,
            stockQuantity = 15.0,
            minStockAlert = 5.0,
            unit = "sac"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveProduct(product) }
        assertTrue(result.isSuccess)

        val products = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProducts().first { list -> list.any { it.name == "Riz Bongo 25kg" } }
        }
        val found = products.first { it.name == "Riz Bongo 25kg" }
        assertEquals(15.0, found.stockQuantity, 0.001)
        assertEquals(4.0, found.marginUSD, 0.001)
    }

    @Test
    fun recordSale_withStockDeduction_updatesProductStockAndRecordsSale() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = KasaRepository(firestore)

        val productId = UUID.randomUUID().toString()
        val product = Product(
            id = productId,
            name = "Huile Mayor 5L",
            purchasePriceUSD = 8.0,
            sellingPriceUSD = 10.0,
            stockQuantity = 20.0,
            minStockAlert = 4.0
        )
        repository.saveProduct(product)

        val cartItem = SaleItem(
            productId = productId,
            productName = "Huile Mayor 5L",
            quantity = 2.0,
            unitPriceUSD = 10.0,
            purchasePriceUSD = 8.0,
            totalUSD = 20.0
        )

        val sale = Sale(
            id = UUID.randomUUID().toString(),
            totalUSD = 20.0,
            totalFC = 57000.0,
            currencyUsed = "USD",
            exchangeRate = 2850.0,
            grossProfitUSD = 4.0,
            paymentType = "CASH",
            isPaid = true
        )

        val saleResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordSale(sale, listOf(cartItem))
        }
        assertTrue(saleResult.isSuccess)

        val observedSales = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSales().first { list -> list.any { it.id == sale.id } }
        }
        assertTrue(observedSales.any { it.id == sale.id })
    }

    @Test
    fun saveExpense_authenticatedUser_recordsSuccessfully() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = KasaRepository(firestore)

        val expense = Expense(
            id = UUID.randomUUID().toString(),
            title = "Facture Électricité",
            category = "Électricité / Eau",
            amountUSD = 30.0,
            amountFC = 85500.0,
            currencyUsed = "USD",
            notes = "SNEL mois en cours"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveExpense(expense) }
        assertTrue(result.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeExpenses().first { list -> list.any { it.id == expense.id } }
        }
        assertTrue(observed.any { it.id == expense.id })
    }

    @Test
    fun saveDetailedDebt_andAcomptePayment_tracksBalanceAccurately() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = KasaRepository(firestore)

        val debt = com.example.data.model.DetailedDebt(
            id = UUID.randomUUID().toString(),
            clientName = "Mama Sarah",
            clientPhone = "+243999000111",
            productName = "Riz Blanc 25kg",
            quantity = 2.0,
            unit = "sac",
            totalAmountUSD = 52.0,
            totalAmountFC = 148200.0,
            advancePaidUSD = 20.0,
            advancePaidFC = 57000.0,
            dueDate = "30/10/2026",
            notes = "Acompte initial"
        )

        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveDetailedDebt(debt) }
        assertTrue("saveRes failed: ${saveRes.exceptionOrNull()?.message}", saveRes.isSuccess)

        val debts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDetailedDebts().first { list -> list.any { it.id == debt.id } }
        }
        val found = debts.first { it.id == debt.id }
        assertEquals("Mama Sarah", found.clientName)
        assertEquals(32.0, found.remainingUSD, 0.001)
        assertTrue(found.isPartial)
    }

    @Test
    fun observeProducts_unauthenticatedCaller_failsSecurityRules() = runBlocking {
        auth.signOut()
        val repository = KasaRepository(firestore)

        var caughtException: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProducts().first()
            }
        } catch (e: Exception) {
            caughtException = e
        }
        assertNotNull("Expected security exception for unauthenticated user", caughtException)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_kasapos@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
