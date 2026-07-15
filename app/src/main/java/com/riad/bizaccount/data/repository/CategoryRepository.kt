package com.riad.bizaccount.data.repository

import com.riad.bizaccount.data.local.AppDatabase
import com.riad.bizaccount.data.local.dao.CategoryDao
import com.riad.bizaccount.data.local.entity.CategoryEntity
import com.riad.bizaccount.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

class CategoryInUseException(val transactionCount: Int) : Exception()

@Singleton
class CategoryRepository @Inject constructor(
    private val dao: CategoryDao
) {
    fun byType(type: TransactionType): Flow<List<CategoryEntity>> = dao.getByType(type)

    fun all(): Flow<List<CategoryEntity>> = dao.getAll()

    suspend fun add(category: CategoryEntity): Long = dao.insert(category)

    suspend fun update(category: CategoryEntity) = dao.update(category)

    suspend fun delete(id: Long) {
        val count = dao.transactionCountForCategory(id)
        if (count > 0) throw CategoryInUseException(count)
        dao.delete(id)
    }

    suspend fun getById(id: Long): CategoryEntity? = dao.getById(id)

    suspend fun allForExport(): List<CategoryEntity> = dao.getAllForExport()

    /** Populates the two default category sets on first launch only. */
    suspend fun seedDefaultsIfEmpty() {
        val existingIncome = dao.getByType(TransactionType.INCOME)
        // Use a direct suspend check instead of collecting the Flow to avoid blocking.
        val hasAny = dao.getAllForExport().isNotEmpty()
        if (hasAny) return

        AppDatabase.DEFAULT_INCOME_CATEGORIES.forEach {
            dao.insert(CategoryEntity(name = it, type = TransactionType.INCOME, isDefault = true))
        }
        AppDatabase.DEFAULT_EXPENSE_CATEGORIES.forEach {
            dao.insert(CategoryEntity(name = it, type = TransactionType.EXPENSE, isDefault = true))
        }
    }
}
