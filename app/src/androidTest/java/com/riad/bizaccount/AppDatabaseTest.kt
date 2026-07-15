package com.riad.bizaccount

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.riad.bizaccount.data.local.AppDatabase
import com.riad.bizaccount.data.local.entity.CategoryEntity
import com.riad.bizaccount.data.local.entity.PaymentMethod
import com.riad.bizaccount.data.local.entity.TransactionEntity
import com.riad.bizaccount.data.local.entity.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insertingTransactionsProducesCorrectPeriodSummary() = runBlocking {
        val categoryId = db.categoryDao().insert(CategoryEntity(name = "বিক্রয়", type = TransactionType.INCOME))
        val today = LocalDate.now().toEpochDay()

        db.transactionDao().insert(
            TransactionEntity(
                type = TransactionType.INCOME,
                dateEpochDay = today,
                timeOfDaySeconds = 3600,
                amountMinor = 100000,
                description = "বিক্রয় ১",
                categoryId = categoryId,
                paymentMethod = PaymentMethod.CASH
            )
        )
        db.transactionDao().insert(
            TransactionEntity(
                type = TransactionType.INCOME,
                dateEpochDay = today,
                timeOfDaySeconds = 7200,
                amountMinor = 50000,
                description = "বিক্রয় ২",
                categoryId = categoryId,
                paymentMethod = PaymentMethod.BANK
            )
        )

        val summary = db.transactionDao().getSummary(today, today).first()
        assertEquals(150000L, summary.totalIncomeMinor)
        assertEquals(0L, summary.totalExpenseMinor)
        assertEquals(2, summary.transactionCount)
    }

    @Test
    fun softDeleteExcludesTransactionFromSummaryAndRestoreBringsItBack() = runBlocking {
        val categoryId = db.categoryDao().insert(CategoryEntity(name = "ক্রয়", type = TransactionType.EXPENSE))
        val today = LocalDate.now().toEpochDay()

        val id = db.transactionDao().insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateEpochDay = today,
                timeOfDaySeconds = 3600,
                amountMinor = 20000,
                description = "ক্রয় ১",
                categoryId = categoryId,
                paymentMethod = PaymentMethod.CASH
            )
        )

        db.transactionDao().softDelete(id)
        val afterDelete = db.transactionDao().getSummary(today, today).first()
        assertEquals(0, afterDelete.transactionCount)

        db.transactionDao().restore(id)
        val afterRestore = db.transactionDao().getSummary(today, today).first()
        assertEquals(1, afterRestore.transactionCount)
    }

    @Test
    fun categoryDeletionIsBlockedWhenTransactionsExist() = runBlocking {
        val categoryId = db.categoryDao().insert(CategoryEntity(name = "ভাড়া", type = TransactionType.EXPENSE))
        db.transactionDao().insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                dateEpochDay = LocalDate.now().toEpochDay(),
                timeOfDaySeconds = 0,
                amountMinor = 500000,
                description = "দোকান ভাড়া",
                categoryId = categoryId,
                paymentMethod = PaymentMethod.BANK
            )
        )
        val count = db.categoryDao().transactionCountForCategory(categoryId)
        assertEquals(1, count)
    }
}
