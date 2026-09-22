package com.ainotes.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ainotes_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent_color")
        val DEFAULT_EXPORT = stringPreferencesKey("default_export")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val PROMPT = stringPreferencesKey("ai_prompt")
        val PERM_ASKED = booleanPreferencesKey("permission_asked")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { p ->
        p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    val accent: Flow<String> = context.dataStore.data.map { it[Keys.ACCENT] ?: "#7C4DFF" }

    val defaultExport: Flow<String> =
        context.dataStore.data.map { it[Keys.DEFAULT_EXPORT] ?: "PDF" }

    val onboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING] ?: false }

    val customPrompt: Flow<String> =
        context.dataStore.data.map { it[Keys.PROMPT] ?: "" }

    suspend fun setTheme(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.THEME] = mode.name }

    suspend fun setAccent(color: String) =
        context.dataStore.edit { it[Keys.ACCENT] = color }

    suspend fun setDefaultExport(format: String) =
        context.dataStore.edit { it[Keys.DEFAULT_EXPORT] = format }

    suspend fun setOnboardingDone() =
        context.dataStore.edit { it[Keys.ONBOARDING] = true }

    suspend fun setCustomPrompt(text: String) =
        context.dataStore.edit { it[Keys.PROMPT] = text }

    val permissionAsked: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.PERM_ASKED] ?: false }

    suspend fun markPermissionAsked() =
        context.dataStore.edit { it[Keys.PERM_ASKED] = true }
}
