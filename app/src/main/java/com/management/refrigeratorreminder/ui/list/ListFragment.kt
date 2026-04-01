package com.management.refrigeratorreminder.ui.list

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.PopupMenu
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
import com.google.android.material.snackbar.Snackbar
import com.management.refrigeratorreminder.R
import com.management.refrigeratorreminder.RefrigeratorReminderApp
import com.management.refrigeratorreminder.databinding.FragmentListBinding
import com.management.refrigeratorreminder.domain.model.ItemCategory
import com.management.refrigeratorreminder.domain.model.StorageType
import com.management.refrigeratorreminder.ui.common.PantryItemAdapter
import com.management.refrigeratorreminder.ui.common.SimpleViewModelFactory
import com.management.refrigeratorreminder.ui.home.HomeFragment
import com.management.refrigeratorreminder.ui.model.ListSortOption
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
            onItemClick = { item ->
                findNavController().navigate(
                    R.id.editItemFragment,
                    bundleOf(HomeFragment.EditItemArgumentKey to item.itemId),
                )
            },
            onActionClick = { item, anchor ->
                showActionsMenu(item, anchor)
            },
        )
    }

    private lateinit var sortOptions: List<Pair<String, ListSortOption>>
    private lateinit var categoryOptions: List<Pair<String, ItemCategory?>>
    private lateinit var storageOptions: List<Pair<String, StorageType?>>
    private lateinit var statusOptions: List<Pair<String, StatusFilterOption>>
    private var rendering = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentListBinding.bind(view)

        binding.recyclerItems.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerItems.adapter = adapter

        sortOptions = listOf(
            getString(R.string.sort_expiry) to ListSortOption.EXPIRY_ASC,
            getString(R.string.sort_name) to ListSortOption.NAME_ASC,
            getString(R.string.sort_created) to ListSortOption.CREATED_DESC,
        )
        categoryOptions = listOf(getString(R.string.filter_all) to null) +
            ItemCategory.values().map { getString(it.labelRes) to it }
        storageOptions = listOf(getString(R.string.filter_all) to null) +
            StorageType.values().map { getString(it.labelRes) to it }
        statusOptions = listOf(
            getString(R.string.status_filter_active) to StatusFilterOption.ACTIVE,
            getString(R.string.status_filter_all) to StatusFilterOption.ALL,
            getString(R.string.status_filter_expired) to StatusFilterOption.EXPIRED,
            getString(R.string.status_filter_today) to StatusFilterOption.TODAY,
            getString(R.string.status_filter_soon) to StatusFilterOption.SOON,
            getString(R.string.status_filter_safe) to StatusFilterOption.SAFE,
            getString(R.string.status_filter_consumed) to StatusFilterOption.CONSUMED,
            getString(R.string.status_filter_discarded) to StatusFilterOption.DISCARDED,
        )

        setupDropdowns()

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
                    binding.dropdownSort.setText(sortOptions.first { it.second == state.sortOption }.first, false)
                    binding.dropdownCategory.setText(categoryOptions.first { it.second == state.categoryFilter }.first, false)
                    binding.dropdownStorage.setText(storageOptions.first { it.second == state.storageFilter }.first, false)
                    binding.dropdownStatus.setText(statusOptions.first { it.second == state.statusFilter }.first, false)
                    rendering = false

                    adapter.submitList(state.items)
                    binding.layoutEmptyList.isVisible = state.isEmpty
                    binding.recyclerItems.isVisible = !state.isEmpty
                }
            }
        }
    }

    private fun setupDropdowns() {
        binding.dropdownSort.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortOptions.map { it.first }))
        binding.dropdownSort.setOnItemClickListener { _, _, position, _ ->
            if (!rendering) viewModel.setSortOption(sortOptions[position].second)
        }

        binding.dropdownCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categoryOptions.map { it.first }))
        binding.dropdownCategory.setOnItemClickListener { _, _, position, _ ->
            if (!rendering) viewModel.setCategoryFilter(categoryOptions[position].second)
        }

        binding.dropdownStorage.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, storageOptions.map { it.first }))
        binding.dropdownStorage.setOnItemClickListener { _, _, position, _ ->
            if (!rendering) viewModel.setStorageFilter(storageOptions[position].second)
        }

        binding.dropdownStatus.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statusOptions.map { it.first }))
        binding.dropdownStatus.setOnItemClickListener { _, _, position, _ ->
            if (!rendering) viewModel.setStatusFilter(statusOptions[position].second)
        }
    }

    private fun showActionsMenu(item: PantryItemPresentation, anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            inflate(R.menu.item_actions_menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        findNavController().navigate(
                            R.id.editItemFragment,
                            bundleOf(HomeFragment.EditItemArgumentKey to item.itemId),
                        )
                        true
                    }

                    R.id.action_consume -> {
                        mutateWithUndo(item.itemId, R.string.message_item_consumed) { viewModel.markConsumed(item.itemId) }
                        true
                    }

                    R.id.action_discard -> {
                        mutateWithUndo(item.itemId, R.string.message_item_discarded) { viewModel.markDiscarded(item.itemId) }
                        true
                    }

                    R.id.action_delete -> {
                        mutateWithUndo(item.itemId, R.string.message_item_deleted) { viewModel.deleteItem(item.itemId) }
                        true
                    }

                    else -> false
                }
            }
        }.show()
    }

    private fun mutateWithUndo(
        itemId: String,
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
