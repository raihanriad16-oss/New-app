package com.riad.bizaccount.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riad.bizaccount.data.settings.AppSettings
import com.riad.bizaccount.data.settings.SettingsDataStore
import com.riad.bizaccount.util.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed class SettingsEvent {
    object BackupSuccess : SettingsEvent()
    object BackupFailure : SettingsEvent()
    object RestoreSuccess : SettingsEvent()
    object RestoreFailure : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val backupManager: BackupManager
) : ViewModel() {

    val settings = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    fun updateBusinessName(name: String) = viewModelScope.launch { settingsDataStore.setBusinessName(name) }
    fun updateCurrencySymbol(symbol: String) = viewModelScope.launch { settingsDataStore.setCurrencySymbol(symbol) }
    fun updateDarkMode(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setDarkMode(enabled) }

    fun backupNow() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            runCatching { backupManager.createBackup() }
                .onSuccess { _events.emit(SettingsEvent.BackupSuccess) }
                .onFailure { _events.emit(SettingsEvent.BackupFailure) }
        }
    }

    fun listBackups(): List<File> = backupManager.listBackups()

    fun restore(file: File) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val ok = runCatching { backupManager.restoreBackup(file) }.getOrDefault(false)
            _events.emit(if (ok) SettingsEvent.RestoreSuccess else SettingsEvent.RestoreFailure)
        }
    }
}
