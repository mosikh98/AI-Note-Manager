package com.ainote.manager.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore by preferencesDataStore(name = "appearance")

data class AppearanceSettings(
    val theme: String = "system",
    val accent: String = "violet",
    val background: String = "default",
    val fontScale: Float = 1f,
)

object AppearancePreferences {
    private val themeKey = stringPreferencesKey("theme")
    private val accentKey = stringPreferencesKey("accent")
    private val backgroundKey = stringPreferencesKey("background")
    private val fontScaleKey = floatPreferencesKey("font_scale")

    fun observe(context: Context): Flow<AppearanceSettings> = context.appearanceDataStore.data.map { prefs ->
        AppearanceSettings(
            theme = prefs[themeKey] ?: "system",
            accent = prefs[accentKey] ?: "violet",
            background = prefs[backgroundKey] ?: "default",
            fontScale = prefs[fontScaleKey] ?: 1f,
        )
    }

    suspend fun update(context: Context, settings: AppearanceSettings) {
        context.appearanceDataStore.edit { prefs ->
            prefs[themeKey] = settings.theme
            prefs[accentKey] = settings.accent
            prefs[backgroundKey] = settings.background
            prefs[fontScaleKey] = settings.fontScale
        }
    }
}