package com.example.recepy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.recepy.data.local.AppDatabase
import com.example.recepy.data.preferences.AppTheme
import com.example.recepy.data.preferences.SettingsPreferences
import com.example.recepy.data.preferences.ThemeMode
import com.example.recepy.ui.screens.extractDomain
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = SettingsPreferences(application)
    private val recipeDao   = AppDatabase.getDatabase(application).recipeDao()

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM
    )

    val appTheme: StateFlow<AppTheme> = preferences.appTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppTheme.ORANGE
    )

    // Map<domain, count> — מקובץ לפי domain, פעם אחת לכל אתר
    val domainCounts: StateFlow<Map<String, Int>> = recipeDao.getAllSourceUrls()
        .map { urls ->
            urls
                .filter { it.isNotBlank() }
                .groupingBy { extractDomain(it) }
                .eachCount()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    val isDeveloper: StateFlow<Boolean> = preferences.isDeveloper.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(themeMode)
        }
    }

    fun updateAppTheme(appTheme: AppTheme) {
        viewModelScope.launch {
            preferences.setAppTheme(appTheme)
        }
    }

    fun setDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setDeveloperMode(enabled)
        }
    }

    // מוחק מתכונים שה-domain שלהם נמצא ברשימה הנבחרת
    fun deleteByDomains(domains: Set<String>) {
        viewModelScope.launch {
            val allUrls = recipeDao.getAllSourceUrlsOnce()
            val urlsToDelete = allUrls.filter { url ->
                extractDomain(url) in domains
            }
            if (urlsToDelete.isNotEmpty()) {
                recipeDao.deleteRecipesBySourceUrls(urlsToDelete)
            }
        }
    }
}