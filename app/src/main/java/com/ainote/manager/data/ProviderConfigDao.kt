package com.ainote.manager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderConfigDao {
    @Query("SELECT * FROM provider_configs ORDER BY id DESC")
    fun observeAll(): Flow<List<ProviderConfigEntity>>

    @Query("SELECT * FROM provider_configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): ProviderConfigEntity?

    @Query("SELECT * FROM provider_configs WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<ProviderConfigEntity?>

    @Insert
    suspend fun insert(config: ProviderConfigEntity): Long

    @Update
    suspend fun update(config: ProviderConfigEntity)

    @Delete
    suspend fun delete(config: ProviderConfigEntity)

    @Query("UPDATE provider_configs SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE provider_configs SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Transaction
    suspend fun activate(id: Long) {
        clearActive()
        setActive(id)
    }
}
