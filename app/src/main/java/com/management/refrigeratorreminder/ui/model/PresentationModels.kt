package com.management.refrigeratorreminder.ui.model

import com.management.refrigeratorreminder.data.local.entity.PantryItemEntity
import com.management.refrigeratorreminder.domain.model.FreshnessStatus
import com.management.refrigeratorreminder.domain.model.ItemCategory
import com.management.refrigeratorreminder.domain.model.StorageType
import java.time.LocalDate
import java.time.LocalDateTime

data class PantryItemPresentation(
    val itemId: String,
    val name: String,
    val category: ItemCategory,
    val storageType: StorageType,
    val expiryDate: LocalDate,
    val freshnessStatus: FreshnessStatus,
    val daysUntilExpiry: Int,
    val quantity: String?,
    val note: String?,
    val createdAt: LocalDateTime,
)

enum class StatusFilterOption {
    ALL,
    ACTIVE,
    EXPIRED,
    TODAY,
    SOON,
    SAFE,
    CONSUMED,
    DISCARDED,
}

fun PantryItemEntity.toPresentation(
    freshnessStatus: FreshnessStatus,
    daysUntilExpiry: Int,
): PantryItemPresentation {
    return PantryItemPresentation(
        itemId = itemId,
        name = name,
        category = category,
        storageType = storageType,
        expiryDate = expiryDate,
        freshnessStatus = freshnessStatus,
        daysUntilExpiry = daysUntilExpiry,
        quantity = quantity,
        note = note,
        createdAt = createdAt,
    )
}
