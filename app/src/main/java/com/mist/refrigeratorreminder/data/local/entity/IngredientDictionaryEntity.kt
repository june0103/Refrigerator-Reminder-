package com.mist.refrigeratorreminder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mist.refrigeratorreminder.domain.model.IngredientSource
import com.mist.refrigeratorreminder.domain.model.ItemCategory
import java.time.LocalDateTime

@Entity(tableName = "ingredient_dictionary")
data class IngredientDictionaryEntity(
    @PrimaryKey val id: String,
    val source: IngredientSource,
    val rawName: String,
    val displayName: String,
    val normalizedName: String,
    val aliases: List<String>,
    val category: ItemCategory,
    val searchPriority: Int,
    val usageCount: Int = 0,
    val lastUsedAt: LocalDateTime? = null,
    val enabled: Boolean = true,
)
