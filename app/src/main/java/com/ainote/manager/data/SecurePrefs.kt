package com.ainote.manager.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Wrapper around EncryptedSharedPreferences (Android Keystore backed) used to store
 * secrets: AI provider API keys and cloud storage API keys. Values here are never
 * written to Room, never logged, and never hard-coded — only entered by the user
 * at runtime through the Settings screens.
 */
class SecurePrefs(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun putProviderApiKey(providerConfigId: Long, apiKey: String) {
        prefs.edit().putString(providerKeyName(providerConfigId), apiKey).apply()
    }

    fun getProviderApiKey(providerConfigId: Long): String? =
        prefs.getString(providerKeyName(providerConfigId), null)

    fun removeProviderApiKey(providerConfigId: Long) {
        prefs.edit().remove(providerKeyName(providerConfigId)).apply()
    }

    fun putCloudApiKey(cloudConfigId: Long, apiKey: String) {
        prefs.edit().putString(cloudKeyName(cloudConfigId), apiKey).apply()
    }

    fun getCloudApiKey(cloudConfigId: Long): String? =
        prefs.getString(cloudKeyName(cloudConfigId), null)

    fun removeCloudApiKey(cloudConfigId: Long) {
        prefs.edit().remove(cloudKeyName(cloudConfigId)).apply()
    }

    private fun providerKeyName(id: Long) = "provider_api_key_$id"
    private fun cloudKeyName(id: Long) = "cloud_api_key_$id"
}
