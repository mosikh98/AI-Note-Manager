package com.ainote.manager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cloud backup destination metadata. The API key is stored encrypted (see SecurePrefs),
 * keyed by [id]; only non-secret fields live here.
 */
@Entity(tableName = "cloud_configs")
data class CloudConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val serverUrl: String,
    val isActive: Boolean = false,
)
