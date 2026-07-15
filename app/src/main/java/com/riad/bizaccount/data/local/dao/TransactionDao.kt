package com.riad.bizaccount.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.riad.bizaccount.data.local.entity.PaymentMethod
import com.riad.bizaccount.data.local.entity.TransactionEntity
import com.riad.bizaccount.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

/** Lightweight aggregate projection used by the dashboard and reports. */
data class PeriodSummary(
    val totalIncomeMinor: Long,
    val totalExpenseMinor: Long,
    val transactionCount: Int
)

data class TransactionWithCategory(
    val id: Long,
    val type: TransactionType,
    val dateEpochDay: Long,
    val timeOfDaySeconds: Int,
    val amountMinor: Long,
    val description: String,
    val categoryId: Long,
    val categoryName: String,
    val categoryColorHex: String,
    val paymentMethod: PaymentMethod,
    val referenceNumber: String?,
    val notes: String?
)

data class CategoryBreakdown(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val totalMinor: Long,
    val count: Int
)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    /** Soft delete so "Undo" can restore instantly without re-entering data. */
    @Query("UPDATE transactions SET isDeleted = 1, updatedAtMillis = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET isDeleted = 0, updatedAtMillis = :now WHERE id = :id")
    suspend fun restore(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM transactions WHERE isDeleted = 1 AND updatedAtMillis < :beforeMillis")
    suspend fun purgeOldDeleted(beforeMillis: Long)

    @Query(
        """
        SELECT t.id, t.type, t.dateEpochDay, t.timeOfDaySeconds, t.amountMinor, t.description,
               t.categoryId, c.name AS categoryName, c.colorHex AS categoryColorHex,
               t.paymentMethod, t.referenceNumber, t.notes
        FROM transactions t
        INNER JOIN categories c ON c.id = t.categoryId
        WHERE t.isDeleted = 0
        ORDER BY t.dateEpochDay DESC, t.timeOfDaySeconds DESC, t.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getRecent(limit: Int, offset: Int): Flow<List<TransactionWithCategory>>

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amountMinor ELSE 0 END), 0) AS totalIncomeMinor,
               COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amountMinor ELSE 0 END), 0) AS totalExpenseMinor,
               COUNT(*) AS transactionCount
        FROM transactions
        WHERE isDeleted = 0 AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        """
    )
    fun getSummary(startEpochDay: Long, endEpochDay: Long): Flow<PeriodSummary>

    @Query(
        """
        SELECT t.id, t.type, t.dateEpochDay, t.timeOfDaySeconds, t.amountMinor, t.description,
               t.categoryId, c.name AS categoryName, c.colorHex AS categoryColorHex,
               t.paymentMethod, t.referenceNumber, t.notes
        FROM transactions t
        INNER JOIN categories c ON c.id = t.categoryId
        WHERE t.isDeleted = 0 AND t.dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY t.dateEpochDay DESC, t.timeOfDaySeconds DESC
        """
    )
    fun getInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<TransactionWithCategory>>

    @Query(
        """
        SELECT c.id AS categoryId, c.name AS categoryName, c.colorHex,
               COALESCE(SUM(t.amountMinor), 0) AS totalMinor, COUNT(t.id) AS count
        FROM categories c
        LEFT JOIN transactions t ON t.categoryId = c.id AND t.isDeleted = 0
            AND t.dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        WHERE c.type = :type
        GROUP BY c.id
        HAVING count > 0
        ORDER BY totalMinor DESC
        """
    )
    fun getCategoryBreakdown(type: TransactionType, startEpochDay: Long, endEpochDay: Long): Flow<List<CategoryBreakdown>>

    @Query(
        """
        SELECT t.id, t.type, t.dateEpochDay, t.timeOfDaySeconds, t.amountMinor, t.description,
               t.categoryId, c.name AS categoryName, c.colorHex AS categoryColorHex,
               t.paymentMethod, t.referenceNumber, t.notes
        FROM transactions t
        INNER JOIN categories c ON c.id = t.categoryId
        WHERE t.isDeleted = 0
          AND (:query = '' OR t.description LIKE '%' || :query || '%' OR t.referenceNumber LIKE '%' || :query || '%')
          AND (:typeFilter IS NULL OR t.type = :typeFilter)
          AND (:categoryFilter IS NULL OR t.categoryId = :categoryFilter)
          AND (:paymentFilter IS NULL OR t.paymentMethod = :paymentFilter)
          AND (:startEpochDay IS NULL OR t.dateEpochDay >= :startEpochDay)
          AND (:endEpochDay IS NULL OR t.dateEpochDay <= :endEpochDay)
          AND (:minAmountMinor IS NULL OR t.amountMinor >= :minAmountMinor)
          AND (:maxAmountMinor IS NULL OR t.amountMinor <= :maxAmountMinor)
        ORDER BY
          CASE WHEN :sortBy = 'DATE_DESC' THEN t.dateEpochDay END DESC,
          CASE WHEN :sortBy = 'DATE_DESC' THEN t.timeOfDaySeconds END DESC,
          CASE WHEN :sortBy = 'DATE_ASC' THEN t.dateEpochDay END ASC,
          CASE WHEN :sortBy = 'AMOUNT_DESC' THEN t.amountMinor END DESC,
          CASE WHEN :sortBy = 'AMOUNT_ASC' THEN t.amountMinor END ASC,
          t.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun search(
        query: String,
        typeFilter: TransactionType?,
        categoryFilter: Long?,
        paymentFilter: PaymentMethod?,
        startEpochDay: Long?,
        endEpochDay: Long?,
        minAmountMinor: Long?,
        maxAmountMinor: Long?,
        sortBy: String,
        limit: Int,
        offset: Int
    ): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE isDeleted = 0")
    suspend fun getAllForExport(): List<TransactionEntity>

    @Query("SELECT MAX(amountMinor) FROM transactions WHERE isDeleted = 0 AND type = :type AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun getMax(type: TransactionType, startEpochDay: Long, endEpochDay: Long): Long?
}
