package com.management.refrigeratorreminder

import android.app.Application
import com.management.refrigeratorreminder.core.AppContainer

class RefrigeratorReminderApp : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
