package com.mist.refrigeratorreminder.domain.search

import com.mist.refrigeratorreminder.data.local.entity.IngredientDictionaryEntity
import com.mist.refrigeratorreminder.domain.model.IngredientSource
import com.mist.refrigeratorreminder.domain.model.ItemCategory
import com.mist.refrigeratorreminder.domain.model.SearchMatchType
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientSearchEngineTest {
    @Test
    fun exact_match_is_ranked_above_partial_match() {
        val results = IngredientSearchEngine.search(
            entries = listOf(
                entry(id = "1", displayName = "양파", normalizedName = "양파"),
                entry(id = "2", displayName = "적양파", normalizedName = "적양파", usageCount = 10),
            ),
            query = "양파",
        )

        assertEquals("양파", results.first().displayName)
        assertEquals(SearchMatchType.EXACT, results.first().matchType)
        assertFalse(results.any { it.isDirectAdd })
    }

    @Test
    fun blank_query_returns_recent_entries_without_direct_add() {
        val results = IngredientSearchEngine.search(
            entries = listOf(
                entry(
                    id = "1",
                    displayName = "양파",
                    normalizedName = "양파",
                    usageCount = 1,
                    lastUsedAt = LocalDateTime.of(2026, 4, 1, 8, 0),
                ),
                entry(
                    id = "2",
                    displayName = "우유",
                    normalizedName = "우유",
                    usageCount = 5,
                    lastUsedAt = LocalDateTime.of(2026, 4, 1, 9, 0),
                ),
            ),
            query = "",
        )

        assertEquals("우유", results.first().displayName)
        assertEquals(SearchMatchType.RECENT, results.first().matchType)
        assertFalse(results.any { it.isDirectAdd })
    }

    @Test
    fun partial_matches_append_direct_add_when_exact_match_is_missing() {
        val results = IngredientSearchEngine.search(
            entries = listOf(
                entry(id = "1", displayName = "콩나물", normalizedName = "콩나물"),
                entry(id = "2", displayName = "콩자반", normalizedName = "콩자반"),
            ),
            query = "콩",
        )

        assertEquals(3, results.size)
        assertEquals("콩나물", results[0].displayName)
        assertEquals("콩자반", results[1].displayName)
        assertTrue(results.last().isDirectAdd)
        assertEquals("콩", results.last().displayName)
        assertEquals(SearchMatchType.DIRECT_ADD, results.last().matchType)
    }

    @Test
    fun exact_match_hides_direct_add_even_when_other_results_exist() {
        val results = IngredientSearchEngine.search(
            entries = listOf(
                entry(id = "1", displayName = "콩", normalizedName = "콩"),
                entry(id = "2", displayName = "콩나물", normalizedName = "콩나물"),
            ),
            query = "콩",
        )

        assertEquals("콩", results.first().displayName)
        assertEquals(SearchMatchType.EXACT, results.first().matchType)
        assertFalse(results.any { it.isDirectAdd })
    }

    @Test
    fun no_matches_return_only_direct_add() {
        val results = IngredientSearchEngine.search(
            entries = listOf(entry(id = "1", displayName = "양파", normalizedName = "양파")),
            query = "콩",
        )

        assertEquals(1, results.size)
        assertTrue(results.first().isDirectAdd)
        assertEquals("콩", results.first().displayName)
    }

    @Test
    fun results_respect_limit_including_direct_add() {
        val results = IngredientSearchEngine.search(
            entries = listOf(
                entry(id = "1", displayName = "우유", normalizedName = "우유"),
                entry(id = "2", displayName = "우동", normalizedName = "우동"),
                entry(id = "3", displayName = "우엉", normalizedName = "우엉"),
                entry(id = "4", displayName = "우거지", normalizedName = "우거지"),
                entry(id = "5", displayName = "우렁", normalizedName = "우렁"),
                entry(id = "6", displayName = "우묵", normalizedName = "우묵"),
            ),
            query = "우",
            limit = 5,
        )

        assertEquals(5, results.size)
        assertTrue(results.last().isDirectAdd)
        assertEquals("우", results.last().displayName)
    }

    private fun entry(
        id: String,
        displayName: String,
        normalizedName: String,
        usageCount: Int = 0,
        lastUsedAt: LocalDateTime? = null,
    ): IngredientDictionaryEntity {
        return IngredientDictionaryEntity(
            id = id,
            source = IngredientSource.MFDS,
            rawName = displayName,
            displayName = displayName,
            normalizedName = normalizedName,
            aliases = emptyList(),
            category = ItemCategory.VEGETABLE,
            searchPriority = 10,
            usageCount = usageCount,
            lastUsedAt = lastUsedAt,
        )
    }
}
