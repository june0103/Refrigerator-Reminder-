package com.management.refrigeratorreminder

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.management.refrigeratorreminder.core.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RefrigeratorReminderApp : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            MobileAds.initialize(this@RefrigeratorReminderApp) {}
        }
    }
}
