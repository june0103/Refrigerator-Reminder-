package com.management.refrigeratorreminder.ui.list

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.management.refrigeratorreminder.R
import com.management.refrigeratorreminder.RefrigeratorReminderApp
import com.management.refrigeratorreminder.databinding.FragmentListBinding
import com.management.refrigeratorreminder.domain.model.ItemCategory
import com.management.refrigeratorreminder.domain.model.StorageType
import com.management.refrigeratorreminder.ui.common.PantryItemDetailBottomSheet
import com.management.refrigeratorreminder.ui.common.PantryItemAdapter
import com.management.refrigeratorreminder.ui.common.SimpleViewModelFactory
import com.management.refrigeratorreminder.ui.home.HomeFragment
import com.management.refrigeratorreminder.ui.model.PantryItemPresentation
import com.management.refrigeratorreminder.ui.model.StatusFilterOption
import kotlinx.coroutines.launch

class ListFragment : Fragment(R.layout.fragment_list) {
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ListViewModel by viewModels {
        val appContainer = (requireActivity().application as RefrigeratorReminderApp).appContainer
        SimpleViewModelFactory {
            ListViewModel(appContainer.pantryRepository, appContainer.settingsRepository)
        }
    }

    private val adapter by lazy {
        PantryItemAdapter(
            showActions = true,
            onItemClick = { item -> showItemDetail(item) },
            onActionClick = { item, _ -> showItemDetail(item) },
        )
    }

    private lateinit var categoryOptions: List<Pair<String, ItemCategory?>>
    private lateinit var storageOptions: List<Pair<String, StorageType?>>
    private var rendering = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentListBinding.bind(view)

        binding.recyclerItems.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerItems.adapter = adapter
        childFragmentManager.setFragmentResultListener(
            PantryItemDetailBottomSheet.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            when (bundle.getString(PantryItemDetailBottomSheet.RESULT_ACTION)) {
                PantryItemDetailBottomSheet.ACTION_EDIT -> {
                    val itemId = bundle.getString(PantryItemDetailBottomSheet.RESULT_ITEM_ID).orEmpty()
                    findNavController().navigate(
                        R.id.editItemFragment,
                        bundleOf(HomeFragment.EditItemArgumentKey to itemId),
                    )
                }

                PantryItemDetailBottomSheet.ACTION_CONSUME -> {
                    handleSheetMutation(bundle, R.string.message_item_consumed) { itemId ->
                        viewModel.markConsumed(itemId)
                    }
                }

                PantryItemDetailBottomSheet.ACTION_DISCARD -> {
                    handleSheetMutation(bundle, R.string.message_item_discarded) { itemId ->
                        viewModel.markDiscarded(itemId)
                    }
                }

                PantryItemDetailBottomSheet.ACTION_DELETE -> {
                    handleSheetMutation(bundle, R.string.message_item_deleted) { itemId ->
                        viewModel.deleteItem(itemId)
                    }
                }
            }
        }

        categoryOptions = listOf(getString(R.string.filter_all) to null) +
            ItemCategory.values().map { getString(it.labelRes) to it }
        storageOptions = listOf(getString(R.string.filter_all) to null) +
            StorageType.values().map { getString(it.labelRes) to it }

        setupFilterBar()

        binding.editSearch.doOnTextChanged { text, _, _, _ ->
            if (!rendering) {
                viewModel.setQuery(text?.toString().orEmpty())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    rendering = true
                    if (binding.editSearch.text?.toString() != state.query) {
                        binding.editSearch.setText(state.query)
                    }
                    renderStatusFilter(state.statusFilter)
                    renderFilterButton(
                        button = binding.buttonStorageFilter,
                        label = getString(R.string.label_storage),
                        value = storageOptions.first { it.second == state.storageFilter }.first,
                        active = state.storageFilter != null,
                    )
                    renderFilterButton(
                        button = binding.buttonCategoryFilter,
                        label = getString(R.string.label_category),
                        value = categoryOptions.first { it.second == state.categoryFilter }.first,
                        active = state.categoryFilter != null,
                    )
                    val hasCustomFilters = state.query.isNotBlank() ||
                        state.categoryFilter != null ||
                        state.storageFilter != null ||
                        state.statusFilter != StatusFilterOption.ACTIVE
                    binding.buttonResetFilters.isVisible = hasCustomFilters
                    rendering = false

                    adapter.submitList(state.items)
                    binding.layoutEmptyList.isVisible = state.isEmpty
                    binding.recyclerItems.isVisible = !state.isEmpty
                }
            }
        }
    }

    private fun setupFilterBar() {
        binding.groupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            if (rendering) return@setOnCheckedStateChangeListener

            val selectedFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_status_expired -> StatusFilterOption.EXPIRED
                R.id.chip_status_all -> StatusFilterOption.ALL
                else -> StatusFilterOption.ACTIVE
            }
            viewModel.setStatusFilter(selectedFilter)
        }

        binding.buttonStorageFilter.setOnClickListener { anchor ->
            showFilterMenu(anchor, storageOptions, viewModel.uiState.value.storageFilter) { selected ->
                viewModel.setStorageFilter(selected)
            }
        }

        binding.buttonCategoryFilter.setOnClickListener { anchor ->
            showFilterMenu(anchor, categoryOptions, viewModel.uiState.value.categoryFilter) { selected ->
                viewModel.setCategoryFilter(selected)
            }
        }

        binding.buttonResetFilters.setOnClickListener {
            if (!rendering) {
                viewModel.resetFilters()
            }
        }
    }

    private fun renderStatusFilter(filterOption: StatusFilterOption) {
        val checkedId = when (filterOption) {
            StatusFilterOption.EXPIRED -> R.id.chip_status_expired
            StatusFilterOption.ALL -> R.id.chip_status_all
            else -> R.id.chip_status_active
        }
        binding.groupStatus.check(checkedId)
    }

    private fun renderFilterButton(
        button: MaterialButton,
        label: String,
        value: String,
        active: Boolean,
    ) {
        button.text = "$label: $value"

        val backgroundColor = if (active) R.color.brand_primary_container else R.color.brand_surface
        val textColor = if (active) R.color.brand_on_primary_container else R.color.brand_on_surface
        val strokeColor = if (active) R.color.brand_primary else R.color.brand_outline

        button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), backgroundColor))
        button.setTextColor(ContextCompat.getColor(requireContext(), textColor))
        button.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), strokeColor))
    }

    private fun <T> showFilterMenu(
        anchor: View,
        options: List<Pair<String, T>>,
        selectedValue: T,
        onSelected: (T) -> Unit,
    ) {
        PopupMenu(requireContext(), anchor).apply {
            options.forEachIndexed { index, (label, value) ->
                val title = if (value == selectedValue) "\u2713 $label" else label
                menu.add(Menu.NONE, index, index, title)
            }
            setOnMenuItemClickListener { item ->
                if (!rendering) {
                    onSelected(options[item.itemId].second)
                }
                true
            }
        }.show()
    }

    private fun showItemDetail(item: PantryItemPresentation) {
        if (childFragmentManager.findFragmentByTag(PantryItemDetailBottomSheet.TAG) != null) return
        PantryItemDetailBottomSheet.newInstance(item)
            .show(childFragmentManager, PantryItemDetailBottomSheet.TAG)
    }

    private fun mutateWithUndo(
        messageResId: Int,
        action: suspend () -> com.management.refrigeratorreminder.data.local.entity.PantryItemEntity?,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val previous = action() ?: return@launch
            Snackbar.make(binding.root, messageResId, Snackbar.LENGTH_LONG)
                .setAction(R.string.button_undo) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.restoreItem(previous)
                    }
                }
                .show()
        }
    }

    private fun handleSheetMutation(
        result: Bundle,
        messageResId: Int,
        action: suspend (String) -> com.management.refrigeratorreminder.data.local.entity.PantryItemEntity?,
    ) {
        val itemId = result.getString(PantryItemDetailBottomSheet.RESULT_ITEM_ID).orEmpty()
        if (itemId.isBlank()) return
        mutateWithUndo(messageResId) { action(itemId) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
