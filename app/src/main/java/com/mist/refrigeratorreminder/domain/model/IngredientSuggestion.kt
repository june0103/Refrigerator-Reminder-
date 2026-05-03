package com.mist.refrigeratorreminder.domain.model

data class IngredientSuggestion(
    val id: String,
    val displayName: String,
    val rawName: String,
    val category: ItemCategory,
    val source: IngredientSource,
    val matchType: SearchMatchType,
    val isDirectAdd: Boolean = false,
)
