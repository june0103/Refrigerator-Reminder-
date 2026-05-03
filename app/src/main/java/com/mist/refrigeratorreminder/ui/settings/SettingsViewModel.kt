package com.mist.refrigeratorreminder.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mist.refrigeratorreminder.data.repository.SettingsRepository
import com.mist.refrigeratorreminder.domain.model.NotificationSettings
import com.mist.refrigeratorreminder.notification.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    val uiState: StateFlow<NotificationSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationSettings(),
    )

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            reminderScheduler.schedule(settingsRepository.getCurrentSettings())
        }
    }

    fun updateLeadDays(leadDays: Int) {
        viewModelScope.launch {
            settingsRepository.setLeadDays(leadDays)
            reminderScheduler.schedule(settingsRepository.getCurrentSettings())
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour, minute)
            reminderScheduler.schedule(settingsRepository.getCurrentSettings())
        }
    }
}
