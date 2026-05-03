package com.mist.refrigeratorreminder.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.refrigeratorreminder.data.repository.PantryRepository
import com.mist.refrigeratorreminder.data.repository.SettingsRepository
import com.mist.refrigeratorreminder.domain.StatusCalculator
import com.mist.refrigeratorreminder.domain.model.FreshnessStatus
import com.mist.refrigeratorreminder.domain.model.StorageType
import com.mist.refrigeratorreminder.ui.model.PantryItemPresentation
import com.mist.refrigeratorreminder.ui.model.toPresentation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val selectedStorageType: StorageType? = null,
    val expiredCount: Int = 0,
    val soonCount: Int = 0,
    val items: List<PantryItemPresentation> = emptyList(),
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

class HomeViewModel(
    private val pantryRepository: PantryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val selectedStorageType = MutableStateFlow<StorageType?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        pantryRepository.observeAllItems(),
        settingsRepository.settingsFlow,
        selectedStorageType,
    ) { items, settings, storageType ->
        val activeItems = items.filter { item ->
            StatusCalculator.calculateStatus(item, settings.leadDays) !in setOf(
                FreshnessStatus.CONSUMED,
                FreshnessStatus.DISCARDED,
                FreshnessStatus.SAFE,
            )
        }
        val presentations = activeItems.map { item ->
            val status = StatusCalculator.calculateStatus(item, settings.leadDays)
            item.toPresentation(status, StatusCalculator.daysUntilExpiry(item))
        }.filter { presentation ->
            storageType == null || presentation.storageType == storageType
        }.sortedWith(
            compareBy<PantryItemPresentation> { severity(it.freshnessStatus) }
                .thenBy { it.expiryDate }
                .thenBy { it.name },
        )

        HomeUiState(
            selectedStorageType = storageType,
            expiredCount = activeItems.count { StatusCalculator.calculateStatus(it, settings.leadDays) == FreshnessStatus.EXPIRED },
            soonCount = activeItems.count {
                val status = StatusCalculator.calculateStatus(it, settings.leadDays)
                status == FreshnessStatus.TODAY || status == FreshnessStatus.SOON
            },
            items = presentations,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun setStorageFilter(storageType: StorageType?) {
        selectedStorageType.value = storageType
    }

    suspend fun markConsumed(itemId: String) = pantryRepository.markConsumed(itemId)

    suspend fun markDiscarded(itemId: String) = pantryRepository.markDiscarded(itemId)

    suspend fun deleteItem(itemId: String) = pantryRepository.deleteItem(itemId)

    suspend fun restoreItem(item: com.mist.refrigeratorreminder.data.local.entity.PantryItemEntity) {
        pantryRepository.restoreItem(item)
    }

    private fun severity(status: FreshnessStatus): Int = when (status) {
        FreshnessStatus.EXPIRED -> 0
        FreshnessStatus.TODAY -> 1
        FreshnessStatus.SOON -> 2
        FreshnessStatus.SAFE -> 3
        FreshnessStatus.CONSUMED -> 4
        FreshnessStatus.DISCARDED -> 5
    }
}
