package com.management.refrigeratorreminder.ui.edit

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.management.refrigeratorreminder.R
import com.management.refrigeratorreminder.data.repository.PantryRepository
import com.management.refrigeratorreminder.domain.model.IngredientSuggestion
import com.management.refrigeratorreminder.domain.model.ItemCategory
import com.management.refrigeratorreminder.domain.model.PantryItemDraft
import com.management.refrigeratorreminder.domain.model.StorageType
import com.management.refrigeratorreminder.util.TextNormalizer
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditItemUiState(
    val itemId: String? = null,
    val name: String = "",
    val category: ItemCategory = ItemCategory.ETC,
    val storageType: StorageType = StorageType.FRIDGE,
    val expiryDate: LocalDate = LocalDate.now().plusDays(3),
    val quantity: String = "",
    val note: String = "",
    val selectedSuggestionId: String? = null,
    val suggestions: List<IngredientSuggestion> = emptyList(),
    val isEditMode: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

sealed interface EditItemEvent {
    data object ItemSaved : EditItemEvent
    data object ItemDeleted : EditItemEvent
    data class ShowMessage(@StringRes val messageRes: Int) : EditItemEvent
}

class EditItemViewModel(
    private val pantryRepository: PantryRepository,
    private val itemId: String?,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(EditItemUiState(itemId = itemId, isEditMode = !itemId.isNullOrBlank()))
    val uiState: StateFlow<EditItemUiState> = mutableUiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = mutableUiState.value,
    )

    private val mutableEvents = MutableSharedFlow<EditItemEvent>()
    val events = mutableEvents.asSharedFlow()

    private var searchJob: Job? = null

    init {
        if (!itemId.isNullOrBlank()) {
            viewModelScope.launch {
                pantryRepository.getItem(itemId)?.let { item ->
                    mutableUiState.value = EditItemUiState(
                        itemId = item.itemId,
                        name = item.name,
                        category = item.category,
                        storageType = item.storageType,
                        expiryDate = item.expiryDate,
                        quantity = item.quantity.orEmpty(),
                        note = item.note.orEmpty(),
                        isEditMode = true,
                    )
                }
            }
        } else {
            refreshSuggestions("")
        }
    }

    fun updateName(value: String) {
        mutableUiState.update { state ->
            val keepSuggestion = state.selectedSuggestionId != null &&
                TextNormalizer.normalize(state.name) == TextNormalizer.normalize(value)
            state.copy(
                name = value,
                selectedSuggestionId = if (keepSuggestion) state.selectedSuggestionId else null,
            )
        }
        refreshSuggestions(value)
    }

    fun updateCategory(value: ItemCategory) {
        mutableUiState.update { it.copy(category = value) }
    }

    fun updateStorageType(value: StorageType) {
        mutableUiState.update { it.copy(storageType = value) }
    }

    fun updateQuantity(value: String) {
        mutableUiState.update { it.copy(quantity = value) }
    }

    fun updateNote(value: String) {
        mutableUiState.update { it.copy(note = value) }
    }

    fun setExpiryDate(date: LocalDate) {
        mutableUiState.update { it.copy(expiryDate = date) }
    }

    fun applyQuickDate(offsetDays: Long) {
        setExpiryDate(LocalDate.now().plusDays(offsetDays))
    }

    fun selectSuggestion(suggestion: IngredientSuggestion) {
        if (suggestion.isDirectAdd) {
            mutableUiState.update {
                it.copy(
                    name = suggestion.displayName,
                    selectedSuggestionId = null,
                    suggestions = emptyList(),
                )
            }
            return
        }

        mutableUiState.update {
            it.copy(
                name = suggestion.displayName,
                category = suggestion.category,
                selectedSuggestionId = suggestion.id,
                suggestions = emptyList(),
            )
        }
    }

    fun saveItem() {
        val state = uiState.value
        if (state.name.isBlank()) {
            viewModelScope.launch {
                mutableEvents.emit(EditItemEvent.ShowMessage(R.string.message_name_required))
            }
            return
        }
        viewModelScope.launch {
            pantryRepository.upsertItem(
                PantryItemDraft(
                    itemId = state.itemId,
                    name = state.name,
                    category = state.category,
                    storageType = state.storageType,
                    expiryDate = state.expiryDate,
                    quantity = state.quantity,
                    note = state.note,
                    suggestionId = state.selectedSuggestionId,
                ),
            )
            mutableEvents.emit(EditItemEvent.ItemSaved)
        }
    }

    fun deleteItem() {
        val currentItemId = uiState.value.itemId ?: return
        viewModelScope.launch {
            pantryRepository.deleteItem(currentItemId)
            mutableEvents.emit(EditItemEvent.ItemDeleted)
        }
    }

    private fun refreshSuggestions(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(120)
            val suggestions = pantryRepository.searchIngredients(query)
            mutableUiState.update { it.copy(suggestions = suggestions) }
        }
    }
}
