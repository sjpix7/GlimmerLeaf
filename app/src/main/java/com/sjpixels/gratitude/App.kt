package com.sjpixels.gratitude

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
        Reminders.reschedule(this)
    }
}
