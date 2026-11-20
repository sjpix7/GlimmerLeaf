package com.sjpixels.gratitude

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.time.Instant
import java.time.ZoneId

/**
 * Reminder schedule: from the first reminder time (default 9:00), every interval (default 3 h),
 * up to the last reminder time (default 21:00), while today's practice is not yet completed.
 * Only the next reminder is ever scheduled; each one schedules the following one when it fires.
 */
object Reminders {
    private const val RC = 4201
    private const val ACTION = "com.sjpixels.gratitude.REMINDER"

    fun slots(p: Prefs): List<Int> {
        val start = p.reminderStartMin.coerceIn(0, 24 * 60 - 1)
        val end = p.reminderEndMin.coerceIn(start, 24 * 60 - 1)
        val step = p.reminderIntervalMin.coerceAtLeast(30)
        val out = ArrayList<Int>()
        var m = start
        while (m <= end) {
            out += m
            m += step
        }
        return out
    }

    fun nextTime(ctx: Context, now: Long = System.currentTimeMillis()): Long? {
        val p = Prefs(ctx)
        if (!p.remindersOn) return null
        val st = Journey.state(ctx)
        if (st.cycle == null || st.current == null) return null
        // A small margin so a reminder that just fired doesn't reschedule itself, without skipping a slot seconds away.
        val notBefore = maxOf(now + 5_000L, st.unlockAt)
        val zone = ZoneId.systemDefault()
        val startDate = Instant.ofEpochMilli(notBefore).atZone(zone).toLocalDate()
        val sl = slots(p)
        for (d in 0..2) {
            val date = startDate.plusDays(d.toLong())
            for (m in sl) {
                val t = date.atTime(m / 60, m % 60).atZone(zone).toInstant().toEpochMilli()
                if (t >= notBefore) return t
            }
        }
        return null
    }

    fun canExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    private fun pending(ctx: Context): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java).setAction(ACTION)
        return PendingIntent.getBroadcast(ctx, RC, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun reschedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(ctx)
        am.cancel(pi)
        val t = nextTime(ctx) ?: return
        try {
            if (canExact(ctx)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
            } else {
                am.setWindow(AlarmManager.RTC_WAKEUP, t, 10 * 60_000L, pi)
            }
        } catch (e: SecurityException) {
            am.setWindow(AlarmManager.RTC_WAKEUP, t, 10 * 60_000L, pi)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val st = Journey.state(context)
        val day = st.current
        if (Prefs(context).remindersOn && st.cycle != null && day != null && !st.isResting()) {
            Notifier.show(context, day)
        }
        Reminders.reschedule(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        Notifier.ensureChannel(context)
        Reminders.reschedule(context)
    }
}

object Notifier {
    const val CHANNEL = "practice_reminders"
    private const val ID = 7001

    fun ensureChannel(ctx: Context) {
        val c = Content.get(ctx, Prefs(ctx).lang)
        val ch = NotificationChannel(CHANNEL, c.t("notif_channel"), NotificationManager.IMPORTANCE_DEFAULT)
        ch.description = c.t("notif_body")
        ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
    }

    fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(ctx: Context, day: Int) {
        if (!canPost(ctx)) return
        ensureChannel(ctx)
        val prefs = Prefs(ctx)
        val c = Content.get(ctx, prefs.lang)
        val d = c.day(day)
        val open = Intent(ctx, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_DAY, day)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(ctx, 100 + day, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val accent = Themes.get(prefs.themeId).light.accent
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accent)
            .setContentTitle(c.t("notif_title", "n" to day, "title" to d.title))
            .setContentText(c.t("notif_body"))
            .setStyle(Notification.BigTextStyle().bigText(d.theme + "\n" + c.t("notif_body")))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        try {
            ctx.getSystemService(NotificationManager::class.java)?.notify(ID, n)
        } catch (e: SecurityException) {
            // Notifications were turned off by the user.
        }
    }

    fun cancel(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java)?.cancel(ID)
    }
}
