package com.management.refrigeratorreminder.domain.model

import java.time.LocalDate

data class PantryItemDraft(
    val itemId: String? = null,
    val name: String,
    val category: ItemCategory,
    val storageType: StorageType,
    val expiryDate: LocalDate,
    val quantity: String? = null,
    val note: String? = null,
    val suggestionId: String? = null,
)
