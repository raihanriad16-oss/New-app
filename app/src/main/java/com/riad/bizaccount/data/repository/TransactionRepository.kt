package com.riad.bizaccount.data.repository

import com.riad.bizaccount.data.local.dao.CategoryBreakdown
import com.riad.bizaccount.data.local.dao.PeriodSummary
import com.riad.bizaccount.data.local.dao.TransactionDao
import com.riad.bizaccount.data.local.dao.TransactionWithCategory
import com.riad.bizaccount.data.local.entity.PaymentMethod
import com.riad.bizaccount.data.local.entity.TransactionEntity
import com.riad.bizaccount.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

enum class SortOption { DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC }

data class TransactionFilters(
    val query: String = "",
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val paymentMethod: PaymentMethod? = null,
    val startEpochDay: Long? = null,
    val endEpochDay: Long? = null,
    val minAmountMinor: Long? = null,
    val maxAmountMinor: Long? = null,
    val sort: SortOption = SortOption.DATE_DESC
)

@Singleton
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao
) {
    suspend fun add(transaction: TransactionEntity): Long = dao.insert(transaction)

    suspend fun update(transaction: TransactionEntity) =
        dao.update(transaction.copy(updatedAtMillis = System.currentTimeMillis()))

    suspend fun softDelete(id: Long) = dao.softDelete(id)

    suspend fun undoDelete(id: Long) = dao.restore(id)

    fun recent(limit: Int = 10, offset: Int = 0): Flow<List<TransactionWithCategory>> =
        dao.getRecent(limit, offset)

    fun summary(startEpochDay: Long, endEpochDay: Long): Flow<PeriodSummary> =
        dao.getSummary(startEpochDay, endEpochDay)

    fun inRange(startEpochDay: Long, endEpochDay: Long): Flow<List<TransactionWithCategory>> =
        dao.getInRange(startEpochDay, endEpochDay)

    fun categoryBreakdown(type: TransactionType, startEpochDay: Long, endEpochDay: Long): Flow<List<CategoryBreakdown>> =
        dao.getCategoryBreakdown(type, startEpochDay, endEpochDay)

    fun search(filters: TransactionFilters, limit: Int = 100, offset: Int = 0): Flow<List<TransactionWithCategory>> =
        dao.search(
            query = filters.query,
            typeFilter = filters.type,
            categoryFilter = filters.categoryId,
            paymentFilter = filters.paymentMethod,
            startEpochDay = filters.startEpochDay,
            endEpochDay = filters.endEpochDay,
            minAmountMinor = filters.minAmountMinor,
            maxAmountMinor = filters.maxAmountMinor,
            sortBy = filters.sort.name,
            limit = limit,
            offset = offset
        )

    suspend fun getById(id: Long): TransactionEntity? = dao.getById(id)

    suspend fun allForExport(): List<TransactionEntity> = dao.getAllForExport()

    suspend fun highest(type: TransactionType, startEpochDay: Long, endEpochDay: Long): Long =
        dao.getMax(type, startEpochDay, endEpochDay) ?: 0L
}
