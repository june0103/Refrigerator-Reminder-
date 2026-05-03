package com.mist.refrigeratorreminder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mist.refrigeratorreminder.data.local.dao.IngredientDictionaryDao
import com.mist.refrigeratorreminder.data.local.dao.PantryItemDao
import com.mist.refrigeratorreminder.data.local.entity.IngredientDictionaryEntity
import com.mist.refrigeratorreminder.data.local.entity.PantryItemEntity

@Database(
    entities = [
        PantryItemEntity::class,
        IngredientDictionaryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pantryItemDao(): PantryItemDao
    abstract fun ingredientDictionaryDao(): IngredientDictionaryDao

    companion object {
        const val DATABASE_NAME = "refrigerator_reminder.db"
    }
}
