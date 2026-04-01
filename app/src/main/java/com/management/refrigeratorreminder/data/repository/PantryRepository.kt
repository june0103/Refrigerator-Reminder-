package com.management.refrigeratorreminder.data.repository

import android.content.Context
import com.management.refrigeratorreminder.data.local.IngredientSeedImporter
import com.management.refrigeratorreminder.data.local.dao.IngredientDictionaryDao
import com.management.refrigeratorreminder.data.local.dao.PantryItemDao
import com.management.refrigeratorreminder.data.local.entity.IngredientDictionaryEntity
import com.management.refrigeratorreminder.data.local.entity.PantryItemEntity
import com.management.refrigeratorreminder.domain.search.IngredientSearchEngine
import com.management.refrigeratorreminder.domain.model.IngredientSource
import com.management.refrigeratorreminder.domain.model.ItemCategory
import com.management.refrigeratorreminder.domain.model.PantryItemDraft
import com.management.refrigeratorreminder.domain.model.PantryItemSourceType
import com.management.refrigeratorreminder.domain.model.PantryLifecycleState
import com.management.refrigeratorreminder.util.TextNormalizer
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

class PantryRepository(
    private val context: Context,
    private val pantryItemDao: PantryItemDao,
    private val ingredientDictionaryDao: IngredientDictionaryDao,
) {
    fun observeAllItems(): Flow<List<PantryItemEntity>> = pantryItemDao.observeAll()

    suspend fun ensureSeedData() {
        if (ingredientDictionaryDao.countAll() > 0) return
        ingredientDictionaryDao.insertAll(IngredientSeedImporter.load(context))
    }

    suspend fun getItem(itemId: String): PantryItemEntity? = pantryItemDao.getById(itemId)

    suspend fun getActiveItemsOnce(): List<PantryItemEntity> = pantryItemDao.getActiveItemsOnce()

    suspend fun upsertItem(draft: PantryItemDraft): PantryItemEntity {
        val now = LocalDateTime.now()
        val normalizedName = TextNormalizer.normalize(draft.name)
        val existing = draft.itemId?.let { pantryItemDao.getById(it) }
        val sourceType = if (draft.suggestionId.isNullOrBlank()) {
            PantryItemSourceType.MANUAL
        } else {
            PantryItemSourceType.DICTIONARY
        }

        val item = existing?.copy(
            name = draft.name.trim(),
            normalizedName = normalizedName,
            category = draft.category,
            storageType = draft.storageType,
            expiryDate = draft.expiryDate,
            quantity = draft.quantity?.trim().orEmpty().ifBlank { null },
            note = draft.note?.trim().orEmpty().ifBlank { null },
            sourceType = sourceType,
            lifecycleState = PantryLifecycleState.ACTIVE,
            updatedAt = now,
            consumedAt = null,
            discardedAt = null,
        ) ?: PantryItemEntity(
            name = draft.name.trim(),
            normalizedName = normalizedName,
            category = draft.category,
            storageType = draft.storageType,
            expiryDate = draft.expiryDate,
            quantity = draft.quantity?.trim().orEmpty().ifBlank { null },
            note = draft.note?.trim().orEmpty().ifBlank { null },
            sourceType = sourceType,
            createdAt = now,
            updatedAt = now,
        )

        pantryItemDao.insert(item)
        recordUsage(item.name, item.category, draft.suggestionId)
        return item
    }

    suspend fun markConsumed(itemId: String): PantryItemEntity? {
        val existing = pantryItemDao.getById(itemId) ?: return null
        val updated = existing.copy(
            lifecycleState = PantryLifecycleState.CONSUMED,
            consumedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        pantryItemDao.update(updated)
        return existing
    }

    suspend fun markDiscarded(itemId: String): PantryItemEntity? {
        val existing = pantryItemDao.getById(itemId) ?: return null
        val updated = existing.copy(
            lifecycleState = PantryLifecycleState.DISCARDED,
            discardedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        pantryItemDao.update(updated)
        return existing
    }

    suspend fun restoreItem(item: PantryItemEntity) {
        pantryItemDao.insert(item.copy(updatedAt = LocalDateTime.now()))
    }

    suspend fun deleteItem(itemId: String): PantryItemEntity? {
        val existing = pantryItemDao.getById(itemId) ?: return null
        pantryItemDao.deleteById(itemId)
        return existing
    }

    suspend fun searchIngredients(query: String): List<com.management.refrigeratorreminder.domain.model.IngredientSuggestion> {
        ensureSeedData()
        return IngredientSearchEngine.search(
            entries = ingredientDictionaryDao.getAllEnabled(),
            query = query,
        )
    }

    private suspend fun recordUsage(
        name: String,
        category: ItemCategory,
        suggestionId: String?,
    ) {
        val now = LocalDateTime.now()
        val normalizedName = TextNormalizer.normalize(name)
        val exactEntry = when {
            !suggestionId.isNullOrBlank() -> ingredientDictionaryDao.getById(suggestionId)
            else -> ingredientDictionaryDao.getAllEnabled().firstOrNull { it.normalizedName == normalizedName }
        }

        if (exactEntry != null) {
            ingredientDictionaryDao.update(
                exactEntry.copy(
                    usageCount = exactEntry.usageCount + 1,
                    lastUsedAt = now,
                ),
            )
            return
        }

        val existingUserEntry = ingredientDictionaryDao.findUserEntryByNormalized(normalizedName)
        if (existingUserEntry != null) {
            ingredientDictionaryDao.update(
                existingUserEntry.copy(
                    displayName = name.trim(),
                    rawName = name.trim(),
                    category = category,
                    usageCount = existingUserEntry.usageCount + 1,
                    lastUsedAt = now,
                ),
            )
        } else {
            ingredientDictionaryDao.insert(
                IngredientDictionaryEntity(
                    id = "user:$normalizedName",
                    source = IngredientSource.USER,
                    rawName = name.trim(),
                    displayName = name.trim(),
                    normalizedName = normalizedName,
                    aliases = emptyList(),
                    category = category,
                    searchPriority = 0,
                    usageCount = 1,
                    lastUsedAt = now,
                ),
            )
        }
    }
}
