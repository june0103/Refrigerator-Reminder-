package com.management.refrigeratorreminder.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.management.refrigeratorreminder.data.repository.PantryRepository
import com.management.refrigeratorreminder.data.repository.SettingsRepository
import com.management.refrigeratorreminder.domain.StatusCalculator
import com.management.refrigeratorreminder.domain.model.FreshnessStatus
import com.management.refrigeratorreminder.domain.model.ItemCategory
import com.management.refrigeratorreminder.domain.model.StorageType
import com.management.refrigeratorreminder.ui.model.PantryItemPresentation
import com.management.refrigeratorreminder.ui.model.StatusFilterOption
import com.management.refrigeratorreminder.ui.model.toPresentation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ListUiState(
    val query: String = "",
    val categoryFilter: ItemCategory? = null,
    val storageFilter: StorageType? = null,
    val statusFilter: StatusFilterOption = StatusFilterOption.ACTIVE,
    val items: List<PantryItemPresentation> = emptyList(),
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

class ListViewModel(
    private val pantryRepository: PantryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<ItemCategory?>(null)
    private val storageFilter = MutableStateFlow<StorageType?>(null)
    private val statusFilter = MutableStateFlow(StatusFilterOption.ACTIVE)

    private val filterState = combine(
        query,
        categoryFilter,
        storageFilter,
        statusFilter,
    ) { searchQuery, category, storage, status ->
        FilterState(
            searchQuery = searchQuery,
            category = category,
            storage = storage,
            status = status,
        )
    }

    val uiState: StateFlow<ListUiState> = combine(
        pantryRepository.observeAllItems(),
        settingsRepository.settingsFlow,
        filterState,
    ) { items, settings, filters ->
        val normalizedQuery = filters.searchQuery.trim()
        val presentations = items.map { item ->
            val freshnessStatus = StatusCalculator.calculateStatus(item, settings.leadDays)
            item.toPresentation(freshnessStatus, StatusCalculator.daysUntilExpiry(item))
        }.filter { presentation ->
            (normalizedQuery.isBlank() || presentation.name.contains(normalizedQuery, ignoreCase = true)) &&
                (filters.category == null || presentation.category == filters.category) &&
                (filters.storage == null || presentation.storageType == filters.storage) &&
                matchesStatus(presentation.freshnessStatus, filters.status)
        }.sortedWith(defaultComparator())

        ListUiState(
            query = filters.searchQuery,
            categoryFilter = filters.category,
            storageFilter = filters.storage,
            statusFilter = filters.status,
            items = presentations,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListUiState(),
    )

    fun setQuery(value: String) {
        query.value = value
    }

    fun setCategoryFilter(value: ItemCategory?) {
        categoryFilter.value = value
    }

    fun setStorageFilter(value: StorageType?) {
        storageFilter.value = value
    }

    fun setStatusFilter(value: StatusFilterOption) {
        statusFilter.value = value
    }

    fun resetFilters() {
        query.value = ""
        categoryFilter.value = null
        storageFilter.value = null
        statusFilter.value = StatusFilterOption.ACTIVE
    }

    private fun matchesStatus(
        status: FreshnessStatus,
        filterOption: StatusFilterOption,
    ): Boolean = when (filterOption) {
        StatusFilterOption.ALL -> true
        StatusFilterOption.ACTIVE -> status !in setOf(FreshnessStatus.CONSUMED, FreshnessStatus.DISCARDED)
        StatusFilterOption.EXPIRED -> status == FreshnessStatus.EXPIRED
        StatusFilterOption.TODAY -> status == FreshnessStatus.TODAY
        StatusFilterOption.SOON -> status == FreshnessStatus.SOON
        StatusFilterOption.SAFE -> status == FreshnessStatus.SAFE
        StatusFilterOption.CONSUMED -> status == FreshnessStatus.CONSUMED
        StatusFilterOption.DISCARDED -> status == FreshnessStatus.DISCARDED
    }

    private fun defaultComparator(): Comparator<PantryItemPresentation> = compareBy<PantryItemPresentation> {
        severity(it.freshnessStatus)
    }.thenBy {
        it.expiryDate
    }.thenByDescending {
        it.createdAt
    }.thenBy {
        it.name
    }

    private fun severity(status: FreshnessStatus): Int = when (status) {
        FreshnessStatus.EXPIRED -> 0
        FreshnessStatus.TODAY -> 1
        FreshnessStatus.SOON -> 2
        FreshnessStatus.SAFE -> 3
        FreshnessStatus.CONSUMED -> 4
        FreshnessStatus.DISCARDED -> 5
    }

    suspend fun markConsumed(itemId: String) = pantryRepository.markConsumed(itemId)

    suspend fun markDiscarded(itemId: String) = pantryRepository.markDiscarded(itemId)

    suspend fun deleteItem(itemId: String) = pantryRepository.deleteItem(itemId)

    suspend fun restoreItem(item: com.management.refrigeratorreminder.data.local.entity.PantryItemEntity) {
        pantryRepository.restoreItem(item)
    }

    private data class FilterState(
        val searchQuery: String,
        val category: ItemCategory?,
        val storage: StorageType?,
        val status: StatusFilterOption,
    )
}
