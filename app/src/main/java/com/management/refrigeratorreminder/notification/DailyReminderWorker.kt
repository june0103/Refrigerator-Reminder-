package com.management.refrigeratorreminder.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.management.refrigeratorreminder.MainActivity
import com.management.refrigeratorreminder.R
import com.management.refrigeratorreminder.RefrigeratorReminderApp
import com.management.refrigeratorreminder.domain.StatusCalculator
import com.management.refrigeratorreminder.domain.model.FreshnessStatus
import kotlinx.coroutines.flow.first

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as RefrigeratorReminderApp
        val settings = app.appContainer.settingsRepository.settingsFlow.first()
        if (!settings.enabled) return Result.success()

        val activeItems = app.appContainer.pantryRepository.getActiveItemsOnce()
        val expiredCount = activeItems.count {
            StatusCalculator.calculateStatus(it, settings.leadDays) == FreshnessStatus.EXPIRED
        }
        val soonCount = activeItems.count {
            val status = StatusCalculator.calculateStatus(it, settings.leadDays)
            status == FreshnessStatus.TODAY || status == FreshnessStatus.SOON
        }

        if (expiredCount == 0 && soonCount == 0) {
            return Result.success()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        createNotificationChannel()

        val contentText = applicationContext.getString(
            R.string.notification_summary_text,
            expiredCount,
            soonCount,
        )
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_HOME, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(applicationContext.getString(R.string.notification_summary_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(ReminderScheduler.NOTIFICATION_ID, notification)

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            ReminderScheduler.CHANNEL_ID,
            applicationContext.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = applicationContext.getString(R.string.notification_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
