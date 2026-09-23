package com.ainote.manager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved AI provider configuration. The API key itself is NOT stored in this Room row —
 * it lives in EncryptedSharedPreferences (see SecurePrefs), keyed by [id]. This table only
 * holds the non-secret metadata plus a flag for which config is currently active.
 */
@Entity(tableName = "provider_configs")
data class ProviderConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val model: String,
    val organizationId: String? = null,
    // JSON-encoded map of extra custom headers, e.g. {"X-Custom":"value"}
    val customHeadersJson: String = "{}",
    val isActive: Boolean = false,
)
