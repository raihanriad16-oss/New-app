package com.riad.bizaccount.util

import android.content.Context
import com.riad.bizaccount.data.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    private fun backupDir(): File =
        File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }

    /**
     * Checkpoints the WAL into the main db file, then copies db + wal + shm to a timestamped
     * backup file so restore always sees a consistent snapshot even mid-write.
     */
    fun createBackup(): File {
        database.query("PRAGMA wal_checkpoint(FULL)", null)
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val target = File(backupDir(), "bizaccount_backup_$stamp.db")
        dbFile.copyTo(target, overwrite = true)
        return target
    }

    fun listBackups(): List<File> =
        backupDir().listFiles { f -> f.extension == "db" }?.sortedByDescending { it.lastModified() } ?: emptyList()

    /**
     * Restores from a backup file. Callers MUST close the current AppDatabase instance and
     * restart the process afterward (e.g. via a "restart app" prompt) since Room does not
     * support hot-swapping the underlying SQLite file for an already-open connection.
     */
    fun restoreBackup(backupFile: File): Boolean {
        if (!backupFile.exists()) return false
        database.close()
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        // Clear any stale WAL/SHM so the restored file is read consistently on next open.
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        return backupFile.copyTo(dbFile, overwrite = true).exists()
    }
}
