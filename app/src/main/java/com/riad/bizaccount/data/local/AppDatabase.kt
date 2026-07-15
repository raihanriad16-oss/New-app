package com.riad.bizaccount.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.riad.bizaccount.data.local.dao.CategoryDao
import com.riad.bizaccount.data.local.dao.TransactionDao
import com.riad.bizaccount.data.local.entity.CategoryEntity
import com.riad.bizaccount.data.local.entity.TransactionEntity
import com.riad.bizaccount.data.local.entity.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "bizaccount.db"

        /**
         * Placeholder for future schema changes. Add real Migration(oldVersion, newVersion)
         * objects here and register them via .addMigrations(...) — never rely on
         * fallbackToDestructiveMigration in production.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Example for the next schema change:
                // db.execSQL("ALTER TABLE transactions ADD COLUMN attachmentPath TEXT")
            }
        }

        fun build(context: Context, scope: CoroutineScope): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch(Dispatchers.IO) {
                            seedDefaultCategories(context, scope)
                        }
                    }
                })
                .build()
        }

        private suspend fun seedDefaultCategories(context: Context, scope: CoroutineScope) {
            // Seeding is done through the instance built above; see CategoryRepository.seedDefaultsIfEmpty()
            // which is invoked from BizAccountApplication on first launch. Kept here as a documented hook.
        }

        val DEFAULT_INCOME_CATEGORIES = listOf("বিক্রয়", "সেবা আয়", "বিবিধ আয়", "বিনিয়োগ")
        val DEFAULT_EXPENSE_CATEGORIES = listOf(
            "ক্রয়", "ভাড়া", "বেতন", "বিদ্যুৎ বিল", "পরিবহন", "খাদ্য", "মেরামত", "বিবিধ ব্যয়"
        )
    }
}
