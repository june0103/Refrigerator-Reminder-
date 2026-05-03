package com.mist.refrigeratorreminder.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mist.refrigeratorreminder.domain.model.NotificationSettings
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class ReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    fun schedule(settings: NotificationSettings) {
        if (!settings.enabled) {
            cancel()
            return
        }

        val now = LocalDateTime.now()
        var nextRun = now.withHour(settings.hour).withMinute(settings.minute).withSecond(0).withNano(0)
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }
        val initialDelay = Duration.between(now, nextRun).toMinutes().coerceAtLeast(1)

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .addTag(WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "daily_reminder_worker"
        const val WORK_TAG = "daily_reminder_tag"
        const val CHANNEL_ID = "expiry_summary_channel"
        const val NOTIFICATION_ID = 1001
    }
}
