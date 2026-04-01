package com.management.refrigeratorreminder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.management.refrigeratorreminder.data.local.entity.IngredientDictionaryEntity

@Dao
interface IngredientDictionaryDao {
    @Query("SELECT COUNT(*) FROM ingredient_dictionary")
    suspend fun countAll(): Int

    @Query("SELECT * FROM ingredient_dictionary WHERE enabled = 1")
    suspend fun getAllEnabled(): List<IngredientDictionaryEntity>

    @Query(
        """
        SELECT * FROM ingredient_dictionary
        WHERE source = 'USER' AND normalizedName = :normalizedName
        LIMIT 1
        """,
    )
    suspend fun findUserEntryByNormalized(normalizedName: String): IngredientDictionaryEntity?

    @Query(
        """
        SELECT * FROM ingredient_dictionary
        WHERE id = :entryId
        LIMIT 1
        """,
    )
    suspend fun getById(entryId: String): IngredientDictionaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<IngredientDictionaryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: IngredientDictionaryEntity)

    @Update
    suspend fun update(entry: IngredientDictionaryEntity)
}
