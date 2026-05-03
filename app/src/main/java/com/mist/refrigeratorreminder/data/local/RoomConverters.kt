package com.mist.refrigeratorreminder.data.local

import androidx.room.TypeConverter
import com.mist.refrigeratorreminder.domain.model.IngredientSource
import com.mist.refrigeratorreminder.domain.model.ItemCategory
import com.mist.refrigeratorreminder.domain.model.PantryItemSourceType
import com.mist.refrigeratorreminder.domain.model.PantryLifecycleState
import com.mist.refrigeratorreminder.domain.model.StorageType
import java.time.LocalDate
import java.time.LocalDateTime

class RoomConverters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    @TypeConverter
    fun fromStringList(value: List<String>?): String = value.orEmpty().joinToString("|")

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("|").filter { it.isNotBlank() }
    }

    @TypeConverter
    fun fromStorageType(value: StorageType?): String? = value?.name

    @TypeConverter
    fun toStorageType(value: String?): StorageType? = value?.let(StorageType::valueOf)

    @TypeConverter
    fun fromItemCategory(value: ItemCategory?): String? = value?.name

    @TypeConverter
    fun toItemCategory(value: String?): ItemCategory? = value?.let(ItemCategory::valueOf)

    @TypeConverter
    fun fromIngredientSource(value: IngredientSource?): String? = value?.name

    @TypeConverter
    fun toIngredientSource(value: String?): IngredientSource? = value?.let(IngredientSource::valueOf)

    @TypeConverter
    fun fromPantryItemSourceType(value: PantryItemSourceType?): String? = value?.name

    @TypeConverter
    fun toPantryItemSourceType(value: String?): PantryItemSourceType? {
        return value?.let(PantryItemSourceType::valueOf)
    }

    @TypeConverter
    fun fromLifecycleState(value: PantryLifecycleState?): String? = value?.name

    @TypeConverter
    fun toLifecycleState(value: String?): PantryLifecycleState? = value?.let(PantryLifecycleState::valueOf)
}
