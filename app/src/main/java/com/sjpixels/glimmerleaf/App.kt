package com.sjpixels.glimmerleaf

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
        Reminders.reschedule(this)
    }
}
