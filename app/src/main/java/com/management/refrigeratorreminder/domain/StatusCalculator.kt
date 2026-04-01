package com.management.refrigeratorreminder.domain

import com.management.refrigeratorreminder.data.local.entity.PantryItemEntity
import com.management.refrigeratorreminder.domain.model.FreshnessStatus
import com.management.refrigeratorreminder.domain.model.PantryLifecycleState
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object StatusCalculator {
    fun calculateStatus(
        item: PantryItemEntity,
        leadDays: Int,
        today: LocalDate = LocalDate.now(),
    ): FreshnessStatus {
        return when (item.lifecycleState) {
            PantryLifecycleState.CONSUMED -> FreshnessStatus.CONSUMED
            PantryLifecycleState.DISCARDED -> FreshnessStatus.DISCARDED
            PantryLifecycleState.ACTIVE -> {
                val daysUntil = ChronoUnit.DAYS.between(today, item.expiryDate).toInt()
                when {
                    daysUntil < 0 -> FreshnessStatus.EXPIRED
                    daysUntil == 0 -> FreshnessStatus.TODAY
                    daysUntil <= leadDays -> FreshnessStatus.SOON
                    else -> FreshnessStatus.SAFE
                }
            }
        }
    }

    fun daysUntilExpiry(item: PantryItemEntity, today: LocalDate = LocalDate.now()): Int {
        return ChronoUnit.DAYS.between(today, item.expiryDate).toInt()
    }
}
