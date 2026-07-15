package com.riad.bizaccount.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.riad.bizaccount.data.local.entity.CategoryEntity
import com.riad.bizaccount.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY type, name")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllForExport(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId AND isDeleted = 0")
    suspend fun transactionCountForCategory(categoryId: Long): Int

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?
}
