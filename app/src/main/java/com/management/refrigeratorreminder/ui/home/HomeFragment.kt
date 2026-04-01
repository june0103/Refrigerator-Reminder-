package com.management.refrigeratorreminder.ui.home

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
import com.management.refrigeratorreminder.R
import com.management.refrigeratorreminder.RefrigeratorReminderApp
import com.management.refrigeratorreminder.databinding.FragmentHomeBinding
import com.management.refrigeratorreminder.domain.model.StorageType
import com.management.refrigeratorreminder.ui.common.PantryItemAdapter
import com.management.refrigeratorreminder.ui.common.SimpleViewModelFactory
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
            onItemClick = { item ->
                findNavController().navigate(
                    R.id.editItemFragment,
                    bundleOf(EditItemArgumentKey to item.itemId),
                )
            },
            onActionClick = { _, _ -> },
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.recyclerHome.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHome.adapter = adapter
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val EditItemArgumentKey = "itemId"
    }
}
