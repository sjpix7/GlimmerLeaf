package com.sjpixels.glimmerleaf

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

data class Cycle(val id: Long, val num: Int, val startedAt: Long, val endedAt: Long?, val status: String) {
    val isActive get() = status == STATUS_ACTIVE

    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_RESET = "reset"
    }
}

data class NoteSection(val prompt: String, val items: List<String>)

data class Notes(val sections: List<NoteSection>, val reflection: String) {
    fun count(): Int = sections.sumOf { s -> s.items.count { it.isNotBlank() } } + (if (reflection.isNotBlank()) 1 else 0)

    fun isEmpty(): Boolean = count() == 0

    fun toJson(): String {
        val o = JSONObject()
        val arr = JSONArray()
        for (s in sections) {
            val so = JSONObject()
            so.put("prompt", s.prompt)
            val items = JSONArray()
            s.items.forEach { items.put(it) }
            so.put("items", items)
            arr.put(so)
        }
        o.put("sections", arr)
        o.put("reflection", reflection)
        return o.toString()
    }

    companion object {
        val EMPTY = Notes(emptyList(), "")

        fun fromJson(s: String?): Notes {
            if (s.isNullOrBlank()) return EMPTY
            return try {
                val o = JSONObject(s)
                val list = ArrayList<NoteSection>()
                o.optJSONArray("sections")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val so = arr.getJSONObject(i)
                        val items = so.optJSONArray("items")
                        val texts = if (items == null) emptyList() else (0 until items.length()).map { items.optString(it) }
                        list += NoteSection(so.optString("prompt"), texts)
                    }
                }
                Notes(list, o.optString("reflection"))
            } catch (e: Exception) {
                EMPTY
            }
        }
    }
}

data class Session(
    val id: Long,
    val cycleId: Long,
    val day: Int,
    val completedAt: Long,
    val durationSec: Int,
    val mood: Int,
    val notes: Notes,
    val noteCount: Int,
)

data class Draft(val notes: Notes, val mood: Int)

/** One day's small wins and thanks. [date] is ISO yyyy-MM-dd in local time. */
data class DailyEntry(val date: String, val wins: List<String>, val thanks: List<String>, val updatedAt: Long) {
    val winCount get() = wins.count { it.isNotBlank() }
    val thanksCount get() = thanks.count { it.isNotBlank() }
}

/** A free-form gratitude journal entry; any number per day. */
data class JournalEntry(val id: Long, val createdAt: Long, val updatedAt: Long, val text: String)

private fun List<String>.toJsonArray(): String = JSONArray().also { a -> forEach { a.put(it) } }.toString()

private fun jsonList(s: String?): List<String> = try {
    val a = JSONArray(s ?: "[]")
    (0 until a.length()).map { a.optString(it) }
} catch (e: Exception) { emptyList() }

/**
 * Local SQLite storage for journeys, completed sessions with notes, unsaved drafts,
 * daily wins and thanks, and the free-form gratitude journal.
 */
class Db private constructor(ctx: Context) : SQLiteOpenHelper(ctx, "gratitude.db", null, 2) {

    companion object {
        @Volatile
        private var inst: Db? = null

        fun get(ctx: Context): Db = inst ?: synchronized(this) {
            inst ?: Db(ctx.applicationContext).also { inst = it }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE cycles (id INTEGER PRIMARY KEY AUTOINCREMENT, num INTEGER NOT NULL, " +
                "started_at INTEGER NOT NULL, ended_at INTEGER, status TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, cycle_id INTEGER NOT NULL, " +
                "day INTEGER NOT NULL, completed_at INTEGER NOT NULL, duration_sec INTEGER NOT NULL DEFAULT 0, " +
                "mood INTEGER NOT NULL DEFAULT 0, notes TEXT NOT NULL DEFAULT '', note_count INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE INDEX idx_sessions_cycle ON sessions(cycle_id, day)")
        db.execSQL(
            "CREATE TABLE drafts (day INTEGER PRIMARY KEY, notes TEXT NOT NULL, mood INTEGER NOT NULL DEFAULT 0, " +
                "updated_at INTEGER NOT NULL)"
        )
        createV2(db)
    }

    /** Version 2: daily wins and thanks, and the gratitude journal. */
    private fun createV2(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS daily (date TEXT PRIMARY KEY, wins TEXT NOT NULL DEFAULT '[]', " +
                "thanks TEXT NOT NULL DEFAULT '[]', updated_at INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS journal (id INTEGER PRIMARY KEY AUTOINCREMENT, created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL, text TEXT NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_journal_created ON journal(created_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createV2(db)
    }

    private fun cycleFrom(c: Cursor): Cycle = Cycle(
        id = c.getLong(0),
        num = c.getInt(1),
        startedAt = c.getLong(2),
        endedAt = if (c.isNull(3)) null else c.getLong(3),
        status = c.getString(4),
    )

    private val cycleCols = "id, num, started_at, ended_at, status"

    fun activeCycle(): Cycle? =
        readableDatabase.rawQuery(
            "SELECT $cycleCols FROM cycles WHERE status = ? ORDER BY started_at DESC LIMIT 1",
            arrayOf(Cycle.STATUS_ACTIVE)
        ).use { c -> if (c.moveToFirst()) cycleFrom(c) else null }

    fun lastCycle(): Cycle? =
        readableDatabase.rawQuery("SELECT $cycleCols FROM cycles ORDER BY started_at DESC LIMIT 1", null)
            .use { c -> if (c.moveToFirst()) cycleFrom(c) else null }

    fun cycles(): List<Cycle> =
        readableDatabase.rawQuery("SELECT $cycleCols FROM cycles ORDER BY started_at DESC", null).use { c ->
            val out = ArrayList<Cycle>()
            while (c.moveToNext()) out += cycleFrom(c)
            out
        }

    fun createCycle(now: Long): Cycle {
        val db = writableDatabase
        val num = db.rawQuery("SELECT COALESCE(MAX(num), 0) + 1 FROM cycles", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 1
        }
        val cv = ContentValues().apply {
            put("num", num)
            put("started_at", now)
            put("status", Cycle.STATUS_ACTIVE)
        }
        val id = db.insert("cycles", null, cv)
        return Cycle(id, num, now, null, Cycle.STATUS_ACTIVE)
    }

    fun endCycle(id: Long, status: String, now: Long) {
        val cv = ContentValues().apply {
            put("status", status)
            put("ended_at", now)
        }
        writableDatabase.update("cycles", cv, "id = ?", arrayOf(id.toString()))
    }

    fun insertSession(cycleId: Long, day: Int, now: Long, durationSec: Int, mood: Int, notes: Notes): Long {
        val cv = ContentValues().apply {
            put("cycle_id", cycleId)
            put("day", day)
            put("completed_at", now)
            put("duration_sec", durationSec)
            put("mood", mood)
            put("notes", notes.toJson())
            put("note_count", notes.count())
        }
        return writableDatabase.insert("sessions", null, cv)
    }

    private val sessionCols = "id, cycle_id, day, completed_at, duration_sec, mood, notes, note_count"

    private fun sessionFrom(c: Cursor): Session = Session(
        id = c.getLong(0),
        cycleId = c.getLong(1),
        day = c.getInt(2),
        completedAt = c.getLong(3),
        durationSec = c.getInt(4),
        mood = c.getInt(5),
        notes = Notes.fromJson(c.getString(6)),
        noteCount = c.getInt(7),
    )

    private fun sessionsQuery(where: String?, args: Array<String>?, order: String): List<Session> {
        val sql = "SELECT $sessionCols FROM sessions" + (if (where != null) " WHERE $where" else "") + " ORDER BY $order"
        return readableDatabase.rawQuery(sql, args).use { c ->
            val out = ArrayList<Session>()
            while (c.moveToNext()) out += sessionFrom(c)
            out
        }
    }

    fun sessions(cycleId: Long): List<Session> =
        sessionsQuery("cycle_id = ?", arrayOf(cycleId.toString()), "day ASC, completed_at ASC")

    fun sessionsForDay(cycleId: Long, day: Int): List<Session> =
        sessionsQuery("cycle_id = ? AND day = ?", arrayOf(cycleId.toString(), day.toString()), "completed_at DESC")

    fun allSessions(): List<Session> = sessionsQuery(null, null, "completed_at ASC")

    /** Index 1..28 holds the number of sessions for that day in the given journey. */
    fun dayCounts(cycleId: Long): IntArray {
        val out = IntArray(Content.DAYS + 1)
        readableDatabase.rawQuery(
            "SELECT day, COUNT(*) FROM sessions WHERE cycle_id = ? GROUP BY day",
            arrayOf(cycleId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val d = c.getInt(0)
                if (d in 1..Content.DAYS) out[d] = c.getInt(1)
            }
        }
        return out
    }

    fun deleteCycle(id: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("sessions", "cycle_id = ?", arrayOf(id.toString()))
            db.delete("cycles", "id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteDay(cycleId: Long, day: Int) {
        writableDatabase.delete("sessions", "cycle_id = ? AND day = ?", arrayOf(cycleId.toString(), day.toString()))
    }

    fun deleteSession(id: Long) {
        writableDatabase.delete("sessions", "id = ?", arrayOf(id.toString()))
    }

    fun draft(day: Int): Draft? =
        readableDatabase.rawQuery("SELECT notes, mood FROM drafts WHERE day = ?", arrayOf(day.toString())).use { c ->
            if (c.moveToFirst()) Draft(Notes.fromJson(c.getString(0)), c.getInt(1)) else null
        }

    fun saveDraft(day: Int, notes: Notes, mood: Int, now: Long) {
        val cv = ContentValues().apply {
            put("day", day)
            put("notes", notes.toJson())
            put("mood", mood)
            put("updated_at", now)
        }
        writableDatabase.insertWithOnConflict("drafts", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun clearDraft(day: Int) {
        writableDatabase.delete("drafts", "day = ?", arrayOf(day.toString()))
    }

    fun clearDrafts() {
        writableDatabase.delete("drafts", null, null)
    }

    // ------------------------------------------------------------ daily wins and thanks

    private fun dailyFrom(c: Cursor) = DailyEntry(c.getString(0), jsonList(c.getString(1)), jsonList(c.getString(2)), c.getLong(3))

    fun daily(date: String): DailyEntry? =
        readableDatabase.rawQuery("SELECT date, wins, thanks, updated_at FROM daily WHERE date = ?", arrayOf(date))
            .use { c -> if (c.moveToFirst()) dailyFrom(c) else null }

    /** Newest day first. */
    fun dailyAll(): List<DailyEntry> =
        readableDatabase.rawQuery("SELECT date, wins, thanks, updated_at FROM daily ORDER BY date DESC", null).use { c ->
            val out = ArrayList<DailyEntry>()
            while (c.moveToNext()) out += dailyFrom(c)
            out
        }

    /** Saves a day's lists; a day with nothing written is removed. */
    fun saveDaily(date: String, wins: List<String>, thanks: List<String>, now: Long) {
        if (wins.all { it.isBlank() } && thanks.all { it.isBlank() }) {
            deleteDaily(date)
            return
        }
        val cv = ContentValues().apply {
            put("date", date)
            put("wins", wins.toJsonArray())
            put("thanks", thanks.toJsonArray())
            put("updated_at", now)
        }
        writableDatabase.insertWithOnConflict("daily", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteDaily(date: String) {
        writableDatabase.delete("daily", "date = ?", arrayOf(date))
    }

    // ------------------------------------------------------------ gratitude journal

    private fun journalFrom(c: Cursor) = JournalEntry(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3))

    fun journalCount(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM journal", null).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun journalCountSince(fromMs: Long): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM journal WHERE created_at >= ?", arrayOf(fromMs.toString()))
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    /** Newest first, [limit] entries. */
    fun journalEntries(limit: Int): List<JournalEntry> =
        readableDatabase.rawQuery(
            "SELECT id, created_at, updated_at, text FROM journal ORDER BY created_at DESC LIMIT ?", arrayOf(limit.toString())
        ).use { c ->
            val out = ArrayList<JournalEntry>()
            while (c.moveToNext()) out += journalFrom(c)
            out
        }

    fun journalEntry(id: Long): JournalEntry? =
        readableDatabase.rawQuery("SELECT id, created_at, updated_at, text FROM journal WHERE id = ?", arrayOf(id.toString()))
            .use { c -> if (c.moveToFirst()) journalFrom(c) else null }

    fun insertJournal(text: String, now: Long): Long {
        val cv = ContentValues().apply {
            put("created_at", now)
            put("updated_at", now)
            put("text", text)
        }
        return writableDatabase.insert("journal", null, cv)
    }

    fun updateJournal(id: Long, text: String, now: Long) {
        val cv = ContentValues().apply {
            put("updated_at", now)
            put("text", text)
        }
        writableDatabase.update("journal", cv, "id = ?", arrayOf(id.toString()))
    }

    fun deleteJournal(id: Long) {
        writableDatabase.delete("journal", "id = ?", arrayOf(id.toString()))
    }
}
