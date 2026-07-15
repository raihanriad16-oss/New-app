package com.riad.bizaccount.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "app_settings")

data class AppSettings(
    val businessName: String = "",
    val currencySymbol: String = "৳",
    val darkMode: Boolean = false
)

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            businessName = prefs[Keys.BUSINESS_NAME] ?: "",
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "৳",
            darkMode = prefs[Keys.DARK_MODE] ?: false
        )
    }

    suspend fun setBusinessName(name: String) {
        context.dataStore.edit { it[Keys.BUSINESS_NAME] = name }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { it[Keys.CURRENCY_SYMBOL] = symbol }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }
}
