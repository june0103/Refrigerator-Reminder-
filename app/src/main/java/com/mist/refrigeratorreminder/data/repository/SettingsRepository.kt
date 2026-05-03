package com.mist.refrigeratorreminder.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mist.refrigeratorreminder.domain.model.NotificationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "reminder_settings")

class SettingsRepository(private val context: Context) {
    val settingsFlow: Flow<NotificationSettings> = context.settingsDataStore.data.map { preferences ->
        NotificationSettings(
            enabled = preferences[KEY_NOTIFICATIONS_ENABLED] ?: false,
            leadDays = preferences[KEY_LEAD_DAYS] ?: 3,
            hour = preferences[KEY_REMINDER_HOUR] ?: 9,
            minute = preferences[KEY_REMINDER_MINUTE] ?: 0,
        )
    }

    suspend fun getCurrentSettings(): NotificationSettings = settingsFlow.first()

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setLeadDays(leadDays: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_LEAD_DAYS] = leadDays
        }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_REMINDER_HOUR] = hour
            preferences[KEY_REMINDER_MINUTE] = minute
        }
    }

    private companion object {
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_LEAD_DAYS = intPreferencesKey("lead_days")
        val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }
}
