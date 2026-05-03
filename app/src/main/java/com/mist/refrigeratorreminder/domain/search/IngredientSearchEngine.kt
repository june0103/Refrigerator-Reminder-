package com.mist.refrigeratorreminder.domain.search

import com.mist.refrigeratorreminder.data.local.entity.IngredientDictionaryEntity
import com.mist.refrigeratorreminder.domain.model.IngredientSource
import com.mist.refrigeratorreminder.domain.model.IngredientSuggestion
import com.mist.refrigeratorreminder.domain.model.ItemCategory
import com.mist.refrigeratorreminder.domain.model.SearchMatchType
import com.mist.refrigeratorreminder.util.TextNormalizer

object IngredientSearchEngine {
    fun search(
        entries: List<IngredientDictionaryEntity>,
        query: String,
        limit: Int = 5,
    ): List<IngredientSuggestion> {
        if (limit <= 0) return emptyList()

        val trimmed = query.trim()
        val normalizedQuery = TextNormalizer.normalize(trimmed)

        if (normalizedQuery.isBlank()) {
            return entries
                .sortedWith(
                    compareByDescending<IngredientDictionaryEntity> { it.lastUsedAt != null }
                        .thenByDescending { it.lastUsedAt }
                        .thenByDescending { it.usageCount }
                        .thenBy { it.searchPriority }
                        .thenBy { it.displayName },
                )
                .take(limit)
                .map { entry -> entry.toSuggestion(SearchMatchType.RECENT) }
        }

        val ranked = entries.mapNotNull { entry ->
            rankEntry(entry, normalizedQuery)?.let { rank -> RankedEntry(rank, entry) }
        }.sortedWith(
            compareBy<RankedEntry> { it.rank.ordinal }
                .thenByDescending { it.entry.lastUsedAt }
                .thenByDescending { it.entry.usageCount }
                .thenBy { it.entry.searchPriority }
                .thenBy { it.entry.displayName },
        )

        val hasExactMatch = ranked.any { it.rank == SearchMatchType.EXACT }
        val suggestionLimit = if (hasExactMatch) limit else (limit - 1).coerceAtLeast(0)
        val suggestions = ranked
            .take(suggestionLimit)
            .map { it.entry.toSuggestion(it.rank) }

        if (hasExactMatch) {
            return suggestions
        }

        return suggestions + buildDirectAddSuggestion(trimmed, normalizedQuery)
    }

    private fun rankEntry(
        entry: IngredientDictionaryEntity,
        normalizedQuery: String,
    ): SearchMatchType? {
        val aliases = entry.aliases.map(TextNormalizer::normalize)
        val names = listOf(entry.normalizedName) + aliases
        return when {
            names.any { it == normalizedQuery } -> SearchMatchType.EXACT
            names.any { it.startsWith(normalizedQuery) } -> SearchMatchType.PREFIX
            names.any { it.contains(normalizedQuery) } -> SearchMatchType.PARTIAL
            else -> null
        }
    }

    private fun IngredientDictionaryEntity.toSuggestion(matchType: SearchMatchType): IngredientSuggestion {
        return IngredientSuggestion(
            id = id,
            displayName = displayName,
            rawName = rawName,
            category = category,
            source = source,
            matchType = matchType,
        )
    }

    private fun buildDirectAddSuggestion(trimmed: String, normalizedQuery: String): IngredientSuggestion {
        val directAddId = normalizedQuery.ifBlank { trimmed }
        return IngredientSuggestion(
            id = "direct:$directAddId",
            displayName = trimmed,
            rawName = trimmed,
            category = ItemCategory.ETC,
            source = IngredientSource.USER,
            matchType = SearchMatchType.DIRECT_ADD,
            isDirectAdd = true,
        )
    }

    private data class RankedEntry(
        val rank: SearchMatchType,
        val entry: IngredientDictionaryEntity,
    )
}
