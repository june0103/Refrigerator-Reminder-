package com.management.refrigeratorreminder.core

import android.content.Context
import androidx.room.Room
import com.management.refrigeratorreminder.data.local.AppDatabase
import com.management.refrigeratorreminder.data.repository.PantryRepository
import com.management.refrigeratorreminder.data.repository.SettingsRepository
import com.management.refrigeratorreminder.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
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

    init {
        appScope.launch {
            pantryRepository.ensureSeedData()
            reminderScheduler.schedule(settingsRepository.settingsFlow.first())
        }
    }
}
