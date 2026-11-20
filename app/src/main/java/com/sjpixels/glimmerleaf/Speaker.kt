package com.sjpixels.glimmerleaf

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

data class VoiceInfo(val voice: Voice, val gender: String?, val label: String)

/**
 * Reads practices aloud with the phone's built-in text-to-speech engine, offline.
 * Only voices installed on the device (no network) are used.
 */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    interface Listener {
        fun onChunk(index: Int)
        fun onFinished()
        fun onPlayingChanged(playing: Boolean)
        fun onLanguageMissing()
    }

    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var failed = false
    private val pending = ArrayList<() -> Unit>()

    var listener: Listener? = null
    var playing = false
        private set
    var index = 0
        private set
    private var chunks: List<String> = emptyList()
    private var token = 0
    private var configuredKey = ""

    override fun onInit(status: Int) {
        main.post {
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts?.setOnUtteranceProgressListener(progress)
                val jobs = ArrayList(pending)
                pending.clear()
                jobs.forEach { it() }
            } else {
                failed = true
                pending.clear()
                listener?.onLanguageMissing()
            }
        }
    }

    private fun whenReady(job: () -> Unit) {
        when {
            ready -> job()
            failed -> listener?.onLanguageMissing()
            else -> pending += job
        }
    }

    private val progress = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            val (tok, i) = parse(utteranceId) ?: return
            main.post {
                if (tok == token && playing) {
                    index = i
                    listener?.onChunk(i)
                }
            }
        }

        override fun onDone(utteranceId: String?) {
            val (tok, i) = parse(utteranceId) ?: return
            main.post {
                if (tok == token && playing && i >= chunks.size - 1) {
                    playing = false
                    index = 0
                    listener?.onPlayingChanged(false)
                    listener?.onFinished()
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            main.post {
                if (playing) {
                    playing = false
                    listener?.onPlayingChanged(false)
                }
            }
        }
    }

    private fun parse(id: String?): Pair<Int, Int>? {
        if (id == null) return null
        val parts = id.split(":")
        if (parts.size != 2) return null
        val a = parts[0].toIntOrNull() ?: return null
        val b = parts[1].toIntOrNull() ?: return null
        return a to b
    }

    // ---------------------------------------------------------------- voices

    fun offlineVoices(lang: String): List<VoiceInfo> {
        val engine = tts ?: return emptyList()
        if (!ready) return emptyList()
        val loc = Content.locale(lang)
        val langs = if (lang == "zh") setOf("zh", "cmn", "yue") else setOf(loc.language)
        val all: Set<Voice> = try {
            engine.voices ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
        return all.asSequence()
            .filter { v ->
                val vl = v.locale ?: return@filter false
                vl.language in langs &&
                    !v.isNetworkConnectionRequired &&
                    (v.features == null || TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in v.features)
            }
            .sortedWith(
                compareByDescending<Voice> { it.locale.country.equals(loc.country, ignoreCase = true) }
                    .thenByDescending { it.quality }
                    .thenBy { it.name }
            )
            .map { v -> VoiceInfo(v, genderOf(v), prettyName(v)) }
            .toList()
    }

    private fun prettyName(v: Voice): String {
        val country = v.locale.displayCountry
        val code = Regex("-x-([a-z0-9]+)-").find(v.name.lowercase(Locale.ROOT))?.groupValues?.get(1)
        return buildString {
            append(v.locale.displayLanguage)
            if (country.isNotBlank()) append(" (").append(country).append(")")
            append(" · ").append(code ?: v.name)
        }
    }

    /** Best-effort gender detection: engines do not report it officially, so known voice names are used. */
    private fun genderOf(v: Voice): String? {
        val n = v.name.lowercase(Locale.ROOT)
        val f = v.features?.joinToString(" ")?.lowercase(Locale.ROOT) ?: ""
        if ("female" in n || "female" in f || "#female" in f) return "female"
        if (Regex("(^|[^a-z])male").containsMatchIn(n) || Regex("(^|[^a-z])male").containsMatchIn(f)) return "male"
        // Samsung voices: ...SMTf00 / ...SMTm00
        Regex("smt([fm])\\d").find(n)?.let { return if (it.groupValues[1] == "f") "female" else "male" }
        // Google voices: xx-yy-x-<code>-local
        val code = Regex("-x-([a-z]{3})").find(n)?.groupValues?.get(1) ?: return null
        return when (code) {
            in FEMALE_CODES -> "female"
            in MALE_CODES -> "male"
            else -> null
        }
    }

    // ---------------------------------------------------------------- configuration

    /** Applies language, voice, gender and speed. Returns false if the language is not installed. */
    private fun applyConfig(prefs: Prefs): Boolean {
        val engine = tts ?: return false
        val lang = prefs.lang
        val key = "$lang|${prefs.voiceGender}|${prefs.voiceName(lang)}|${prefs.speechRate}"
        engine.setSpeechRate(prefs.speechRate)
        if (key == configuredKey) return true
        val result = engine.setLanguage(Content.locale(lang))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            configuredKey = ""
            return false
        }
        val voices = offlineVoices(lang)
        val wanted = prefs.voiceName(lang)
        val specific = voices.firstOrNull { it.voice.name == wanted }
        val byGender = voices.firstOrNull { it.gender == prefs.voiceGender }
        val chosen = specific ?: byGender
        if (chosen != null) {
            try {
                engine.voice = chosen.voice
            } catch (e: Exception) {
                // keep the engine default
            }
            engine.setPitch(1.0f)
        } else {
            // No voice of the requested gender is installed: gently shift the default voice's pitch.
            engine.setPitch(if (prefs.voiceGender == "male") 0.8f else 1.08f)
        }
        configuredKey = key
        return true
    }

    fun invalidateConfig() {
        configuredKey = ""
    }

    // ---------------------------------------------------------------- playback

    fun play(prefs: Prefs, items: List<String>, from: Int = 0) {
        whenReady {
            if (!applyConfig(prefs)) {
                listener?.onLanguageMissing()
                return@whenReady
            }
            chunks = items
            speakFrom(from.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
        }
    }

    private fun speakFrom(i: Int) {
        val engine = tts ?: return
        if (chunks.isEmpty()) return
        token++
        index = i
        playing = true
        listener?.onPlayingChanged(true)
        listener?.onChunk(i)
        val params = Bundle()
        for (k in i until chunks.size) {
            val mode = if (k == i) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(chunks[k], mode, params, "$token:$k")
        }
    }

    fun pause() {
        if (!playing) return
        token++
        playing = false
        tts?.stop()
        listener?.onPlayingChanged(false)
    }

    fun resume(prefs: Prefs) {
        if (chunks.isEmpty()) return
        play(prefs, chunks, index)
    }

    fun next(prefs: Prefs) {
        if (chunks.isEmpty()) return
        play(prefs, chunks, (index + 1).coerceAtMost(chunks.size - 1))
    }

    fun previous(prefs: Prefs) {
        if (chunks.isEmpty()) return
        play(prefs, chunks, (index - 1).coerceAtLeast(0))
    }

    fun stop() {
        token++
        val was = playing
        playing = false
        index = 0
        tts?.stop()
        if (was) listener?.onPlayingChanged(false)
    }

    fun isLoaded(items: List<String>): Boolean = chunks == items

    fun sample(prefs: Prefs, text: String, voice: Voice? = null) {
        whenReady {
            stop()
            if (!applyConfig(prefs)) {
                listener?.onLanguageMissing()
                return@whenReady
            }
            if (voice != null) {
                try {
                    tts?.voice = voice
                    tts?.setPitch(1.0f)
                } catch (e: Exception) {
                }
                configuredKey = ""
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "sample")
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
        }
        tts = null
        ready = false
    }

    companion object {
        // Google speech-services voice codes (best effort; voices with unknown gender can still be picked manually).
        private val FEMALE_CODES = setOf(
            "sfg", "tpf", "tpc", "iob", "iog", "gba", "gbc", "gbg", "fis", "ahp", "cxx", "ena", "enc", "aua", "auc",
            "hia", "hic", "dea", "deb", "nfh", "eea", "eec", "ana", "esc", "esf", "fra", "frc", "vlf", "caa", "cac",
            "htm", "jab", "ism", "kob", "ruc", "rue", "dfc", "ccc", "cce", "ssa", "yuc", "yue",
        )
        private val MALE_CODES = setOf(
            "iol", "iom", "tpd", "gbb", "gbd", "rjs", "end", "ene", "aub", "aud", "hid", "hie", "ded", "deg",
            "eed", "eef", "esd", "frb", "frd", "cab", "cad", "jac", "jad", "koc", "kod", "rud", "ruf", "ccd", "yud",
        )
    }
}
