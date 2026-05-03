package com.mist.refrigeratorreminder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mist.refrigeratorreminder.data.local.entity.PantryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryItemDao {
    @Query("SELECT * FROM pantry_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_items ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<PantryItemEntity>

    @Query("SELECT * FROM pantry_items WHERE lifecycleState = 'ACTIVE' ORDER BY expiryDate ASC, updatedAt DESC")
    suspend fun getActiveItemsOnce(): List<PantryItemEntity>

    @Query("SELECT * FROM pantry_items WHERE itemId = :itemId LIMIT 1")
    suspend fun getById(itemId: String): PantryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PantryItemEntity)

    @Update
    suspend fun update(item: PantryItemEntity)

    @Query("DELETE FROM pantry_items WHERE itemId = :itemId")
    suspend fun deleteById(itemId: String)
}
