package com.management.refrigeratorreminder.ui.edit

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.management.refrigeratorreminder.R
import com.management.refrigeratorreminder.RefrigeratorReminderApp
import com.management.refrigeratorreminder.databinding.FragmentEditItemBinding
import com.management.refrigeratorreminder.domain.model.ItemCategory
import com.management.refrigeratorreminder.domain.model.StorageType
import com.management.refrigeratorreminder.ui.common.IngredientSuggestionAdapter
import com.management.refrigeratorreminder.ui.common.SimpleViewModelFactory
import com.management.refrigeratorreminder.ui.common.UiFormatter
import com.management.refrigeratorreminder.ui.home.HomeFragment
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch

class EditItemFragment : Fragment(R.layout.fragment_edit_item) {
    private var _binding: FragmentEditItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditItemViewModel by viewModels {
        val appContainer = (requireActivity().application as RefrigeratorReminderApp).appContainer
        SimpleViewModelFactory {
            EditItemViewModel(
                pantryRepository = appContainer.pantryRepository,
                itemId = arguments?.getString(HomeFragment.EditItemArgumentKey),
            )
        }
    }

    private val suggestionAdapter by lazy {
        IngredientSuggestionAdapter { suggestion ->
            viewModel.selectSuggestion(suggestion)
        }
    }

    private lateinit var categoryOptions: List<Pair<String, ItemCategory>>
    private lateinit var storageOptions: List<Pair<String, StorageType>>
    private var rendering = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditItemBinding.bind(view)

        categoryOptions = ItemCategory.values().map { getString(it.labelRes) to it }
        storageOptions = StorageType.values().map { getString(it.labelRes) to it }

        binding.recyclerSuggestions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSuggestions.adapter = suggestionAdapter
        binding.dropdownItemCategory.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categoryOptions.map { it.first }),
        )
        binding.dropdownItemStorage.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, storageOptions.map { it.first }),
        )

        binding.editName.doOnTextChanged { text, _, _, _ ->
            if (!rendering) viewModel.updateName(text?.toString().orEmpty())
        }
        binding.editQuantity.doOnTextChanged { text, _, _, _ ->
            if (!rendering) viewModel.updateQuantity(text?.toString().orEmpty())
        }
        binding.editNote.doOnTextChanged { text, _, _, _ ->
            if (!rendering) viewModel.updateNote(text?.toString().orEmpty())
        }

        binding.dropdownItemCategory.setOnItemClickListener { _, _, position, _ ->
            if (!rendering) viewModel.updateCategory(categoryOptions[position].second)
        }
        binding.dropdownItemStorage.setOnItemClickListener { _, _, position, _ ->
            if (!rendering) viewModel.updateStorageType(storageOptions[position].second)
        }

        binding.buttonQuickToday.setOnClickListener { viewModel.applyQuickDate(0) }
        binding.buttonQuickTomorrow.setOnClickListener { viewModel.applyQuickDate(1) }
        binding.buttonQuickThreeDays.setOnClickListener { viewModel.applyQuickDate(3) }
        binding.buttonQuickSevenDays.setOnClickListener { viewModel.applyQuickDate(7) }
        binding.buttonDatePicker.setOnClickListener { showDatePicker() }
        binding.buttonSave.setOnClickListener { viewModel.saveItem() }
        binding.buttonDelete.setOnClickListener { viewModel.deleteItem() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        rendering = true
                        if (binding.editName.text?.toString() != state.name) binding.editName.setText(state.name)
                        if (binding.editQuantity.text?.toString() != state.quantity) binding.editQuantity.setText(state.quantity)
                        if (binding.editNote.text?.toString() != state.note) binding.editNote.setText(state.note)
                        binding.dropdownItemCategory.setText(categoryOptions.first { it.second == state.category }.first, false)
                        binding.dropdownItemStorage.setText(storageOptions.first { it.second == state.storageType }.first, false)
                        binding.buttonDatePicker.text = UiFormatter.formatDate(state.expiryDate)
                        binding.buttonSave.isEnabled = state.canSave
                        binding.buttonDelete.isVisible = state.isEditMode
                        binding.recyclerSuggestions.isVisible = state.suggestions.isNotEmpty()
                        binding.textSuggestionsLabel.isVisible = state.suggestions.isNotEmpty()
                        suggestionAdapter.submitList(state.suggestions)
                        rendering = false
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is EditItemEvent.ShowMessage -> {
                                Snackbar.make(binding.root, getString(event.messageRes), Snackbar.LENGTH_SHORT).show()
                            }
                            EditItemEvent.ItemSaved -> {
                                Snackbar.make(binding.root, R.string.message_item_saved, Snackbar.LENGTH_SHORT).show()
                                if (!findNavController().popBackStack(R.id.homeFragment, false)) {
                                    findNavController().navigate(R.id.homeFragment)
                                }
                            }

                            EditItemEvent.ItemDeleted -> {
                                Snackbar.make(binding.root, R.string.message_item_deleted, Snackbar.LENGTH_SHORT).show()
                                if (!findNavController().popBackStack()) {
                                    findNavController().navigate(R.id.homeFragment)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val state = viewModel.uiState.value
        val initialSelection = state.expiryDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        MaterialDatePicker.Builder.datePicker()
            .setSelection(initialSelection)
            .build()
            .also { picker ->
                picker.addOnPositiveButtonClickListener { selection ->
                    val date = Instant.ofEpochMilli(selection)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    viewModel.setExpiryDate(date)
                }
            }
            .show(parentFragmentManager, "expiry_date_picker")
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
