package com.mist.refrigeratorreminder

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.mist.refrigeratorreminder.core.AppContainer
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RefrigeratorReminderApp : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mobileAdsInitialized = AtomicBoolean(false)

    fun initializeMobileAdsIfNeeded() {
        if (!mobileAdsInitialized.compareAndSet(false, true)) return
        appScope.launch {
            MobileAds.initialize(this@RefrigeratorReminderApp) {}
        }
    }
}
