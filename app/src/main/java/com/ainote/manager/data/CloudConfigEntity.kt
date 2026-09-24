package com.ainote.manager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CloudProviderType {
    CUSTOM_SERVER,
    GOOGLE_DRIVE,
    DROPBOX,
}

/**
 * Cloud backup destination metadata. Secrets (Dropbox access token, custom-server API key)
 * are stored encrypted (see SecurePrefs), keyed by [id]; only non-secret fields live here.
 * Google Drive doesn't use a pasted secret — [accountLabel] just shows which Google account
 * is currently connected (via on-device Sign-In), the token itself is fetched fresh each time.
 */
@Entity(tableName = "cloud_configs")
data class CloudConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val providerType: String = CloudProviderType.CUSTOM_SERVER.name,
    val serverUrl: String = "",
    val accountLabel: String? = null,
    val isActive: Boolean = false,
) {
    val provider: CloudProviderType
        get() = try {
            CloudProviderType.valueOf(providerType)
        } catch (e: Exception) {
            CloudProviderType.CUSTOM_SERVER
        }
}
