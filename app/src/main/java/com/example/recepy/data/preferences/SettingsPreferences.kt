package com.example.recepy.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppTheme {
    ORANGE,
    BLUE,
    GREEN,
    PINK,
    PURPLE
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsPreferences(
    private val context: Context
) {

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { preferences ->
        val storedValue = preferences[THEME_MODE_KEY]
        runCatching {
            if (storedValue.isNullOrBlank()) ThemeMode.SYSTEM else ThemeMode.valueOf(storedValue)
        }.getOrDefault(ThemeMode.SYSTEM)
    }

    val appTheme: Flow<AppTheme> = context.settingsDataStore.data.map { preferences ->
        val storedValue = preferences[APP_THEME_KEY]
        runCatching {
            if (storedValue.isNullOrBlank()) AppTheme.ORANGE else AppTheme.valueOf(storedValue)
        }.getOrDefault(AppTheme.ORANGE)
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode.name
        }
    }

    suspend fun setAppTheme(appTheme: AppTheme) {
        context.settingsDataStore.edit { preferences ->
            preferences[APP_THEME_KEY] = appTheme.name
        }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val APP_THEME_KEY = stringPreferencesKey("app_theme")
    }
}
