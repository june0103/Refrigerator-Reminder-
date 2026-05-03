package com.mist.refrigeratorreminder.core

import android.content.Context
import androidx.room.Room
import com.mist.refrigeratorreminder.RefrigeratorReminderApp
import com.mist.refrigeratorreminder.core.privacy.PrivacyConsentManager
import com.mist.refrigeratorreminder.data.local.AppDatabase
import com.mist.refrigeratorreminder.data.repository.PantryRepository
import com.mist.refrigeratorreminder.data.repository.SettingsRepository
import com.mist.refrigeratorreminder.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val app = applicationContext as RefrigeratorReminderApp
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        ).fallbackToDestructiveMigration().build()
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(applicationContext)
    }

    val pantryRepository: PantryRepository by lazy {
        PantryRepository(
            context = applicationContext,
            pantryItemDao = database.pantryItemDao(),
            ingredientDictionaryDao = database.ingredientDictionaryDao(),
        )
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(applicationContext)
    }

    val privacyConsentManager: PrivacyConsentManager by lazy {
        PrivacyConsentManager(applicationContext) {
            app.initializeMobileAdsIfNeeded()
        }
    }

    init {
        appScope.launch {
            pantryRepository.ensureSeedData()
            reminderScheduler.schedule(settingsRepository.settingsFlow.first())
        }
    }
}
