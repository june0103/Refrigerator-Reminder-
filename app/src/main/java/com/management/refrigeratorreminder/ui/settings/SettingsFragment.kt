package com.management.refrigeratorreminder.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.management.refrigeratorreminder.R
import com.management.refrigeratorreminder.RefrigeratorReminderApp
import com.management.refrigeratorreminder.databinding.FragmentSettingsBinding
import com.management.refrigeratorreminder.ui.common.SimpleViewModelFactory
import com.management.refrigeratorreminder.ui.common.UiFormatter
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        val appContainer = (requireActivity().application as RefrigeratorReminderApp).appContainer
        SimpleViewModelFactory {
            SettingsViewModel(appContainer.settingsRepository, appContainer.reminderScheduler)
        }
    }

    private var rendering = false
    private val leadDayValues = listOf(1, 2, 3)
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.updateNotificationsEnabled(true)
        } else {
            binding.switchNotifications.isChecked = false
            Snackbar.make(binding.root, R.string.message_permission_denied, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        val leadDayLabels = leadDayValues.map { getString(R.string.lead_days_format, it) }
        binding.dropdownLeadDays.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, leadDayLabels),
        )
        binding.dropdownLeadDays.setOnItemClickListener { _, _, position, _ ->
            if (!rendering) {
                viewModel.updateLeadDays(leadDayValues[position])
            }
        }
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (rendering) return@setOnCheckedChangeListener
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.updateNotificationsEnabled(isChecked)
            }
        }
        binding.buttonTimePicker.setOnClickListener {
            val state = viewModel.uiState.value
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    viewModel.updateReminderTime(hourOfDay, minute)
                },
                state.hour,
                state.minute,
                true,
            ).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    rendering = true
                    binding.switchNotifications.isChecked = state.enabled
                    binding.dropdownLeadDays.setText(getString(R.string.lead_days_format, state.leadDays), false)
                    binding.buttonTimePicker.text = UiFormatter.formatReminderTime(state.hour, state.minute)
                    rendering = false
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
