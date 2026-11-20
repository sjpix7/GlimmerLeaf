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

/** Local SQLite storage for journeys, completed sessions with notes, and unsaved drafts. */
class Db private constructor(ctx: Context) : SQLiteOpenHelper(ctx, "gratitude.db", null, 1) {

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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 is the first schema. Add migrations here in future versions.
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
}
