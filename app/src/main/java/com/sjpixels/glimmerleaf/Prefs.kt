package com.sjpixels.glimmerleaf

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** All user settings, stored on the device. */
class Prefs(context: Context) {
    private val app = context.applicationContext
    private val sp = app.getSharedPreferences("gj_settings", Context.MODE_PRIVATE)

    var themeId: String
        get() = sp.getString("theme", "gold") ?: "gold"
        set(v) { sp.edit().putString("theme", v).apply() }

    var dark: Boolean
        get() = if (sp.contains("dark")) sp.getBoolean("dark", false) else systemDark()
        set(v) { sp.edit().putBoolean("dark", v).apply() }

    private fun systemDark(): Boolean =
        (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    var lang: String
        get() = sp.getString("lang", null) ?: defaultLang()
        set(v) { sp.edit().putString("lang", v).apply() }

    private fun defaultLang(): String {
        val l = Locale.getDefault().language
        return if (l in Content.LANGS) l else "en"
    }

    /** Show the day of the month inside each square of the activity grid. */
    var activityDates: Boolean
        get() = sp.getBoolean("activity_dates", false)
        set(v) { sp.edit().putBoolean("activity_dates", v).apply() }

    /** "female" or "male". */
    var voiceGender: String
        get() = sp.getString("voice_gender", "female") ?: "female"
        set(v) { sp.edit().putString("voice_gender", v).apply() }

    fun voiceName(lang: String): String? = sp.getString("voice_name_$lang", null)

    fun setVoiceName(lang: String, name: String?) {
        val e = sp.edit()
        if (name == null) e.remove("voice_name_$lang") else e.putString("voice_name_$lang", name)
        e.apply()
    }

    var speechRate: Float
        get() = sp.getFloat("speech_rate", 0.95f)
        set(v) { sp.edit().putFloat("speech_rate", v).apply() }

    var remindersOn: Boolean
        get() = sp.getBoolean("reminders_on", true)
        set(v) { sp.edit().putBoolean("reminders_on", v).apply() }

    /** Minutes after midnight of the first reminder (default 9:00). */
    var reminderStartMin: Int
        get() = sp.getInt("reminder_start", 9 * 60)
        set(v) { sp.edit().putInt("reminder_start", v).apply() }

    /** Minutes after midnight of the last possible reminder (default 21:00). */
    var reminderEndMin: Int
        get() = sp.getInt("reminder_end", 21 * 60)
        set(v) { sp.edit().putInt("reminder_end", v).apply() }

    /** Minutes between reminders (default 3 hours). */
    var reminderIntervalMin: Int
        get() = sp.getInt("reminder_interval", 180)
        set(v) { sp.edit().putInt("reminder_interval", v).apply() }

    /** "next_day": the next practice starts the following morning. "immediate": right after completing. */
    var unlockMode: String
        get() = sp.getString("unlock_mode", "next_day") ?: "next_day"
        set(v) { sp.edit().putString("unlock_mode", v).apply() }

    /** Epoch millis before which the next practice is resting (no reminders). 0 = available now. */
    var unlockAt: Long
        get() = sp.getLong("unlock_at", 0L)
        set(v) { sp.edit().putLong("unlock_at", v).apply() }

    var askedNotifications: Boolean
        get() = sp.getBoolean("asked_notif", false)
        set(v) { sp.edit().putBoolean("asked_notif", v).apply() }
}
