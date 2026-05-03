package com.mist.refrigeratorreminder.domain.model

import androidx.annotation.StringRes
import com.mist.refrigeratorreminder.R

enum class StorageType(@StringRes val labelRes: Int) {
    FRIDGE(R.string.storage_fridge),
    FREEZER(R.string.storage_freezer),
    ROOM(R.string.storage_room),
}

enum class ItemCategory(@StringRes val labelRes: Int) {
    VEGETABLE(R.string.category_vegetable),
    FRUIT(R.string.category_fruit),
    DAIRY(R.string.category_dairy),
    MEAT(R.string.category_meat),
    SEAFOOD(R.string.category_seafood),
    DRINK(R.string.category_drink),
    SIDE_DISH(R.string.category_side_dish),
    SAUCE(R.string.category_sauce),
    ETC(R.string.category_etc),
}

enum class IngredientSource {
    MFDS,
    SEED,
    USER,
}

enum class PantryItemSourceType {
    DICTIONARY,
    MANUAL,
}

enum class PantryLifecycleState {
    ACTIVE,
    CONSUMED,
    DISCARDED,
}

enum class FreshnessStatus(@StringRes val labelRes: Int) {
    EXPIRED(R.string.status_expired),
    TODAY(R.string.status_today),
    SOON(R.string.status_soon),
    SAFE(R.string.status_safe),
    CONSUMED(R.string.status_consumed),
    DISCARDED(R.string.status_discarded),
}

enum class SearchMatchType {
    EXACT,
    PREFIX,
    PARTIAL,
    RECENT,
    DIRECT_ADD,
}
