package com.mist.refrigeratorreminder.domain

import com.mist.refrigeratorreminder.data.local.entity.PantryItemEntity
import com.mist.refrigeratorreminder.domain.model.FreshnessStatus
import com.mist.refrigeratorreminder.domain.model.ItemCategory
import com.mist.refrigeratorreminder.domain.model.PantryItemSourceType
import com.mist.refrigeratorreminder.domain.model.PantryLifecycleState
import com.mist.refrigeratorreminder.domain.model.StorageType
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusCalculatorTest {
    private val today = LocalDate.of(2026, 4, 1)

    @Test
    fun expired_item_returns_expired() {
        val item = testItem(expiryDate = today.minusDays(1))

        assertEquals(FreshnessStatus.EXPIRED, StatusCalculator.calculateStatus(item, leadDays = 3, today = today))
    }

    @Test
    fun today_item_returns_today() {
        val item = testItem(expiryDate = today)

        assertEquals(FreshnessStatus.TODAY, StatusCalculator.calculateStatus(item, leadDays = 3, today = today))
    }

    @Test
    fun future_item_inside_lead_days_returns_soon() {
        val item = testItem(expiryDate = today.plusDays(2))

        assertEquals(FreshnessStatus.SOON, StatusCalculator.calculateStatus(item, leadDays = 3, today = today))
    }

    @Test
    fun future_item_outside_lead_days_returns_safe() {
        val item = testItem(expiryDate = today.plusDays(5))

        assertEquals(FreshnessStatus.SAFE, StatusCalculator.calculateStatus(item, leadDays = 3, today = today))
    }

    @Test
    fun consumed_item_overrides_date_status() {
        val item = testItem(
            expiryDate = today.minusDays(10),
            lifecycleState = PantryLifecycleState.CONSUMED,
        )

        assertEquals(FreshnessStatus.CONSUMED, StatusCalculator.calculateStatus(item, leadDays = 3, today = today))
    }

    @Test
    fun discarded_item_overrides_date_status() {
        val item = testItem(
            expiryDate = today.plusDays(1),
            lifecycleState = PantryLifecycleState.DISCARDED,
        )

        assertEquals(FreshnessStatus.DISCARDED, StatusCalculator.calculateStatus(item, leadDays = 3, today = today))
    }

    private fun testItem(
        expiryDate: LocalDate,
        lifecycleState: PantryLifecycleState = PantryLifecycleState.ACTIVE,
    ): PantryItemEntity {
        return PantryItemEntity(
            itemId = "item-1",
            name = "양파",
            normalizedName = "양파",
            category = ItemCategory.VEGETABLE,
            storageType = StorageType.FRIDGE,
            expiryDate = expiryDate,
            sourceType = PantryItemSourceType.DICTIONARY,
            lifecycleState = lifecycleState,
            createdAt = LocalDateTime.of(2026, 4, 1, 9, 0),
            updatedAt = LocalDateTime.of(2026, 4, 1, 9, 0),
        )
    }
}
