package com.mist.refrigeratorreminder.ui.home

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mist.refrigeratorreminder.R
import com.mist.refrigeratorreminder.RefrigeratorReminderApp
import com.mist.refrigeratorreminder.databinding.FragmentHomeBinding
import com.mist.refrigeratorreminder.domain.model.StorageType
import com.mist.refrigeratorreminder.ui.common.PantryItemDetailBottomSheet
import com.mist.refrigeratorreminder.ui.common.PantryItemAdapter
import com.mist.refrigeratorreminder.ui.common.SimpleViewModelFactory
import com.mist.refrigeratorreminder.ui.model.PantryItemPresentation
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val appContainer = (requireActivity().application as RefrigeratorReminderApp).appContainer
        SimpleViewModelFactory {
            HomeViewModel(appContainer.pantryRepository, appContainer.settingsRepository)
        }
    }

    private val adapter by lazy {
        PantryItemAdapter(
            showActions = false,
            onItemClick = { item -> showItemDetail(item) },
            onActionClick = { _, _ -> },
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.recyclerHome.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHome.adapter = adapter
        childFragmentManager.setFragmentResultListener(
            PantryItemDetailBottomSheet.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            when (bundle.getString(PantryItemDetailBottomSheet.RESULT_ACTION)) {
                PantryItemDetailBottomSheet.ACTION_EDIT -> {
                    val itemId = bundle.getString(PantryItemDetailBottomSheet.RESULT_ITEM_ID).orEmpty()
                    findNavController().navigate(
                        R.id.editItemFragment,
                        bundleOf(EditItemArgumentKey to itemId),
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
        binding.buttonEmptyAdd.setOnClickListener {
            findNavController().navigate(R.id.addItemFragment)
        }

        binding.chipStorageAll.setOnClickListener { viewModel.setStorageFilter(null) }
        binding.chipStorageFridge.setOnClickListener { viewModel.setStorageFilter(StorageType.FRIDGE) }
        binding.chipStorageFreezer.setOnClickListener { viewModel.setStorageFilter(StorageType.FREEZER) }
        binding.chipStorageRoom.setOnClickListener { viewModel.setStorageFilter(StorageType.ROOM) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.textExpiredCount.text = state.expiredCount.toString()
                    binding.textDueSoonCount.text = state.soonCount.toString()
                    adapter.submitList(state.items)
                    binding.layoutEmptyHome.isVisible = state.isEmpty
                    binding.recyclerHome.isVisible = !state.isEmpty
                }
            }
        }
    }

    private fun showItemDetail(item: PantryItemPresentation) {
        if (childFragmentManager.findFragmentByTag(PantryItemDetailBottomSheet.TAG) != null) return
        PantryItemDetailBottomSheet.newInstance(item)
            .show(childFragmentManager, PantryItemDetailBottomSheet.TAG)
    }

    private fun handleSheetMutation(
        result: Bundle,
        messageResId: Int,
        action: suspend (String) -> com.mist.refrigeratorreminder.data.local.entity.PantryItemEntity?,
    ) {
        val itemId = result.getString(PantryItemDetailBottomSheet.RESULT_ITEM_ID).orEmpty()
        if (itemId.isBlank()) return
        mutateWithUndo(messageResId) { action(itemId) }
    }

    private fun mutateWithUndo(
        messageResId: Int,
        action: suspend () -> com.mist.refrigeratorreminder.data.local.entity.PantryItemEntity?,
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

    companion object {
        const val EditItemArgumentKey = "itemId"
    }
}
