package com.mist.refrigeratorreminder.ui.common

import android.content.Context
import com.mist.refrigeratorreminder.R
import com.mist.refrigeratorreminder.domain.model.FreshnessStatus
import com.mist.refrigeratorreminder.domain.model.ItemCategory
import com.mist.refrigeratorreminder.domain.model.StorageType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object UiFormatter {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun formatDays(context: Context, status: FreshnessStatus, daysUntil: Int): String {
        return when (status) {
            FreshnessStatus.EXPIRED -> context.getString(R.string.item_days_overdue, kotlin.math.abs(daysUntil))
            FreshnessStatus.TODAY -> context.getString(R.string.item_days_today)
            FreshnessStatus.SOON,
            FreshnessStatus.SAFE,
            -> context.getString(R.string.item_days_remaining, daysUntil)

            FreshnessStatus.CONSUMED,
            FreshnessStatus.DISCARDED,
            -> context.getString(status.labelRes)
        }
    }

    fun formatMeta(
        context: Context,
        category: ItemCategory,
        storageType: StorageType,
        quantity: String?,
    ): String {
        val parts = buildList {
            add(context.getString(category.labelRes))
            add(context.getString(storageType.labelRes))
            if (!quantity.isNullOrBlank()) {
                add(context.getString(R.string.item_quantity_value, quantity))
            }
        }
        return parts.joinToString(" · ")
    }

    fun formatReminderTime(hour: Int, minute: Int): String = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
}
