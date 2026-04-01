package com.management.refrigeratorreminder.data.local

import android.content.Context
import com.management.refrigeratorreminder.data.local.entity.IngredientDictionaryEntity
import com.management.refrigeratorreminder.domain.model.IngredientSource
import com.management.refrigeratorreminder.domain.model.ItemCategory
import com.management.refrigeratorreminder.util.TextNormalizer
import org.json.JSONArray

object IngredientSeedImporter {
    private const val ASSET_FILE_NAME = "ingredient_dictionary_seed.json"

    fun load(context: Context): List<IngredientDictionaryEntity> {
        val rawJson = context.assets.open(ASSET_FILE_NAME).bufferedReader().use { it.readText() }
        val array = JSONArray(rawJson)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val displayName = item.getString("displayName")
                val aliases = buildList {
                    val aliasArray = item.optJSONArray("aliases") ?: JSONArray()
                    for (aliasIndex in 0 until aliasArray.length()) {
                        add(aliasArray.getString(aliasIndex))
                    }
                }
                add(
                    IngredientDictionaryEntity(
                        id = item.getString("id"),
                        source = item.optString("source", IngredientSource.SEED.name).let(IngredientSource::valueOf),
                        rawName = item.optString("rawName", displayName),
                        displayName = displayName,
                        normalizedName = item.optString("normalizedName")
                            .takeIf { it.isNotBlank() }
                            ?: TextNormalizer.normalize(displayName),
                        aliases = aliases,
                        category = item.optString("category", ItemCategory.ETC.name).let(ItemCategory::valueOf),
                        searchPriority = item.optInt("searchPriority", 100),
                        enabled = item.optBoolean("enabled", true),
                    ),
                )
            }
        }
    }
}
