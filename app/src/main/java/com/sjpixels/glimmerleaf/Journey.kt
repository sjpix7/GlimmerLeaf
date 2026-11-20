package com.sjpixels.glimmerleaf

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class DayStatus { DONE, CURRENT, UPCOMING, NONE }

data class JourneyState(
    val cycle: Cycle?,
    val counts: IntArray,
    /** The next practice in order, or null when there is no journey or all 28 are done. */
    val current: Int?,
    val unlockAt: Long,
    /** The most recent journey, set only when no journey is active. */
    val lastCycle: Cycle?,
) {
    val doneCount: Int get() = (1..Content.DAYS).count { counts[it] > 0 }

    /** True while the next practice is resting until tomorrow's first reminder. */
    fun isResting(now: Long = System.currentTimeMillis()): Boolean = cycle != null && current != null && unlockAt > now

    fun statusOf(day: Int): DayStatus = when {
        cycle == null -> DayStatus.NONE
        counts[day] > 0 -> DayStatus.DONE
        day == current -> DayStatus.CURRENT
        else -> DayStatus.UPCOMING
    }
}

data class CompleteResult(val day: Int, val firstTime: Boolean, val cycleCompleted: Boolean)

/** The journey rules: sequential order, early completion, repeats, completion, reset. */
object Journey {

    fun state(ctx: Context): JourneyState {
        val db = Db.get(ctx)
        val c = db.activeCycle()
        val counts = if (c != null) db.dayCounts(c.id) else IntArray(Content.DAYS + 1)
        val current = if (c == null) null else (1..Content.DAYS).firstOrNull { counts[it] == 0 }
        return JourneyState(c, counts, current, Prefs(ctx).unlockAt, if (c == null) db.lastCycle() else null)
    }

    fun start(ctx: Context): Cycle {
        val db = Db.get(ctx)
        db.activeCycle()?.let { return it }
        Prefs(ctx).unlockAt = 0L
        val c = db.createCycle(System.currentTimeMillis())
        Reminders.reschedule(ctx)
        return c
    }

    fun complete(ctx: Context, day: Int, notes: Notes, mood: Int, durationSec: Int): CompleteResult {
        val db = Db.get(ctx)
        val prefs = Prefs(ctx)
        val now = System.currentTimeMillis()
        val cycle = db.activeCycle() ?: start(ctx)
        val before = db.dayCounts(cycle.id)
        val wasCurrent = (1..Content.DAYS).firstOrNull { before[it] == 0 } == day
        val firstTime = before[day] == 0

        db.insertSession(cycle.id, day, now, durationSec, mood, notes)
        db.clearDraft(day)

        val after = db.dayCounts(cycle.id)
        val allDone = (1..Content.DAYS).all { after[it] > 0 }
        if (allDone) {
            db.endCycle(cycle.id, Cycle.STATUS_COMPLETED, now)
            prefs.unlockAt = 0L
            db.clearDrafts()
        } else if (wasCurrent && firstTime) {
            prefs.unlockAt = if (prefs.unlockMode == "immediate") 0L else tomorrowAtStart(prefs)
        }
        Notifier.cancel(ctx)
        Reminders.reschedule(ctx)
        return CompleteResult(day, firstTime, allDone)
    }

    /** Ends the active journey (kept in history as "reset") and starts the practices fresh. */
    fun reset(ctx: Context) {
        val db = Db.get(ctx)
        db.activeCycle()?.let { db.endCycle(it.id, Cycle.STATUS_RESET, System.currentTimeMillis()) }
        Prefs(ctx).unlockAt = 0L
        db.clearDrafts()
        Notifier.cancel(ctx)
        Reminders.reschedule(ctx)
    }

    fun deleteCycle(ctx: Context, id: Long) {
        val db = Db.get(ctx)
        val wasActive = db.activeCycle()?.id == id
        db.deleteCycle(id)
        if (wasActive) {
            Prefs(ctx).unlockAt = 0L
            Notifier.cancel(ctx)
        }
        Reminders.reschedule(ctx)
    }

    fun deleteDay(ctx: Context, cycleId: Long, day: Int) {
        Db.get(ctx).deleteDay(cycleId, day)
        Reminders.reschedule(ctx)
    }

    fun deleteSession(ctx: Context, id: Long) {
        Db.get(ctx).deleteSession(id)
        Reminders.reschedule(ctx)
    }

    fun tomorrowAtStart(prefs: Prefs): Long {
        val m = prefs.reminderStartMin
        return LocalDate.now().plusDays(1).atTime(m / 60, m % 60)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** Moves a pending unlock to the new first-reminder time on the same date, after the user changes that time. */
    fun realignUnlock(prefs: Prefs) {
        val at = prefs.unlockAt
        if (at <= System.currentTimeMillis()) return
        val m = prefs.reminderStartMin
        prefs.unlockAt = localDate(at).atTime(m / 60, m % 60)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun localDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    fun millisOf(d: LocalDate): Long = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
