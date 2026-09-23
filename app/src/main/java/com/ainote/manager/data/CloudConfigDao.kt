package com.ainote.manager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudConfigDao {
    @Query("SELECT * FROM cloud_configs ORDER BY id DESC")
    fun observeAll(): Flow<List<CloudConfigEntity>>

    @Query("SELECT * FROM cloud_configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): CloudConfigEntity?

    @Insert
    suspend fun insert(config: CloudConfigEntity): Long

    @Update
    suspend fun update(config: CloudConfigEntity)

    @Delete
    suspend fun delete(config: CloudConfigEntity)

    @Query("UPDATE cloud_configs SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE cloud_configs SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Transaction
    suspend fun activate(id: Long) {
        clearActive()
        setActive(id)
    }
}
