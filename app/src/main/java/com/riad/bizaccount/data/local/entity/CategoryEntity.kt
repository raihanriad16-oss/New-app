package com.riad.bizaccount.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val colorHex: String = "#1B6B4A",
    val isDefault: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
