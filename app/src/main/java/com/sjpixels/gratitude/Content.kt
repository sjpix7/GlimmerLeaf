package com.sjpixels.gratitude

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class JournalSection(val prompt: String, val count: Int, val multiline: Boolean)

data class PracticeDay(
    val day: Int,
    val title: String,
    val theme: String,
    val intro: List<String>,
    val steps: List<String>,
    val journal: List<JournalSection>,
    val affirmation: String,
)

data class Ritual(
    val id: String,
    val title: String,
    val whenText: String,
    val text: String,
    val prompt: String?,
    val count: Int,
)

/**
 * One language's worth of practices and interface text, loaded from assets/content/<lang>.json.
 * Replace those files to change the practices (keep the same structure).
 */
class ContentPack(
    val lang: String,
    val journeyTitle: String,
    val journeySubtitle: String,
    val weeks: List<String>,
    val rituals: List<Ritual>,
    val days: List<PracticeDay>,
    private val ui: Map<String, String>,
    private val fallback: ContentPack?,
) {
    fun t(key: String): String = ui[key] ?: fallback?.t(key) ?: key

    fun t(key: String, vararg args: Pair<String, Any?>): String {
        var s = t(key)
        for ((k, v) in args) s = s.replace("{$k}", v?.toString() ?: "")
        return s
    }

    fun list(key: String): List<String> = t(key).split("|").map { it.trim() }.filter { it.isNotEmpty() }

    fun day(n: Int): PracticeDay = days[(n - 1).coerceIn(0, days.size - 1)]

    fun weekTitle(day: Int): String = weeks.getOrElse((day - 1) / 7) { "" }

    fun ritual(id: String): Ritual? = rituals.firstOrNull { it.id == id }

    /** The day's own journal prompts, followed by the morning ritual's list. */
    fun journalFor(d: PracticeDay): List<JournalSection> {
        val m = rituals.firstOrNull { it.id == "morning" && it.prompt != null && it.count > 0 }
        return if (m != null) d.journal + JournalSection(m.prompt!!, m.count, false) else d.journal
    }
}

object Content {
    const val DAYS = 28
    val LANGS = listOf("en", "hi", "de", "es", "ja", "ru", "fr", "zh", "ko")
    val NATIVE = mapOf(
        "en" to "English",
        "hi" to "हिन्दी",
        "de" to "Deutsch",
        "es" to "Español",
        "ja" to "日本語",
        "ru" to "Русский",
        "fr" to "Français",
        "zh" to "中文",
        "ko" to "한국어",
    )

    fun locale(lang: String): Locale = Locale.forLanguageTag(
        when (lang) {
            "en" -> "en-US"
            "hi" -> "hi-IN"
            "de" -> "de-DE"
            "es" -> "es-ES"
            "ja" -> "ja-JP"
            "ru" -> "ru-RU"
            "fr" -> "fr-FR"
            "zh" -> "zh-CN"
            "ko" -> "ko-KR"
            else -> "en-US"
        }
    )

    private val cache = HashMap<String, ContentPack>()

    @Synchronized
    fun get(ctx: Context, lang: String): ContentPack {
        cache[lang]?.let { return it }
        val fb = if (lang == "en") null else get(ctx, "en")
        val pack = try {
            parse(lang, load(ctx, lang), fb)
        } catch (e: Exception) {
            fb ?: throw e
        }
        cache[lang] = pack
        return pack
    }

    private fun load(ctx: Context, lang: String): String =
        ctx.assets.open("content/$lang.json").bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun strings(a: JSONArray?): List<String> {
        if (a == null) return emptyList()
        return (0 until a.length()).map { a.getString(it) }
    }

    private fun parse(lang: String, json: String, fb: ContentPack?): ContentPack {
        val root = JSONObject(json)
        val meta = root.optJSONObject("meta") ?: JSONObject()
        val rituals = ArrayList<Ritual>()
        root.optJSONArray("rituals")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                rituals += Ritual(
                    id = o.getString("id"),
                    title = o.optString("title"),
                    whenText = o.optString("when"),
                    text = o.optString("text"),
                    prompt = if (o.has("prompt")) o.getString("prompt") else null,
                    count = o.optInt("count", 0),
                )
            }
        }
        val days = ArrayList<PracticeDay>()
        val arr = root.getJSONArray("days")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val journal = ArrayList<JournalSection>()
            o.optJSONArray("journal")?.let { j ->
                for (k in 0 until j.length()) {
                    val s = j.getJSONObject(k)
                    journal += JournalSection(s.getString("prompt"), s.optInt("count", 1).coerceIn(1, 20), s.optBoolean("multiline", false))
                }
            }
            days += PracticeDay(
                day = o.optInt("day", i + 1),
                title = o.getString("title"),
                theme = o.optString("theme"),
                intro = strings(o.optJSONArray("intro")),
                steps = strings(o.optJSONArray("steps")),
                journal = journal,
                affirmation = o.optString("affirmation"),
            )
        }
        val ui = HashMap<String, String>()
        root.optJSONObject("ui")?.let { u ->
            val keys = u.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                ui[k] = u.getString(k)
            }
        }
        return ContentPack(
            lang = lang,
            journeyTitle = meta.optString("journeyTitle", fb?.journeyTitle ?: ""),
            journeySubtitle = meta.optString("journeySubtitle", fb?.journeySubtitle ?: ""),
            weeks = strings(meta.optJSONArray("weeks")).ifEmpty { fb?.weeks ?: emptyList() },
            rituals = rituals.ifEmpty { fb?.rituals ?: emptyList() },
            days = days,
            ui = ui,
            fallback = fb,
        )
    }
}
