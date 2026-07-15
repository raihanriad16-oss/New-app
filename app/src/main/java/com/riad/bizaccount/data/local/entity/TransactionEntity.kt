package com.riad.bizaccount.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType { INCOME, EXPENSE }

enum class PaymentMethod { CASH, BANK, MOBILE_BANKING }

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("categoryId"),
        Index("dateEpochDay"),
        Index("type")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    /** Days since epoch (LocalDate.toEpochDay) — enables fast indexed date-range queries. */
    val dateEpochDay: Long,
    /** Seconds since midnight, local time. */
    val timeOfDaySeconds: Int,
    /** Stored in minor currency units (poisha) to avoid floating point errors. */
    val amountMinor: Long,
    val description: String,
    val categoryId: Long,
    val paymentMethod: PaymentMethod,
    val referenceNumber: String? = null,
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
