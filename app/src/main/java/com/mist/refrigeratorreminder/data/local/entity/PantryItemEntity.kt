package com.mist.refrigeratorreminder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mist.refrigeratorreminder.domain.model.ItemCategory
import com.mist.refrigeratorreminder.domain.model.PantryItemSourceType
import com.mist.refrigeratorreminder.domain.model.PantryLifecycleState
import com.mist.refrigeratorreminder.domain.model.StorageType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "pantry_items",
    indices = [
        Index(value = ["normalizedName"]),
        Index(value = ["expiryDate"]),
        Index(value = ["lifecycleState"]),
    ],
)
data class PantryItemEntity(
    @PrimaryKey val itemId: String = UUID.randomUUID().toString(),
    val name: String,
    val normalizedName: String,
    val category: ItemCategory,
    val storageType: StorageType,
    val expiryDate: LocalDate,
    val quantity: String? = null,
    val note: String? = null,
    val sourceType: PantryItemSourceType,
    val lifecycleState: PantryLifecycleState = PantryLifecycleState.ACTIVE,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val consumedAt: LocalDateTime? = null,
    val discardedAt: LocalDateTime? = null,
)
