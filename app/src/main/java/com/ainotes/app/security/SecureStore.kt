package com.ainotes.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Keystore-backed encrypted preferences. API keys are never plaintext, never logged. */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "ainotes_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun put(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun get(key: String): String? = prefs.getString(key, null)
    fun remove(key: String) = prefs.edit().remove(key).apply()
}
