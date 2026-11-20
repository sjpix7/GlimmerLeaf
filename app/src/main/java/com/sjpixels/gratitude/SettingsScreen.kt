package com.sjpixels.gratitude

import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class SettingsScreen(act: MainActivity) : Screen(act) {

    override fun build(ui: Ui): View {
        val c = ui.c
        val p = ui.p
        val prefs = act.prefs
        val page = ui.screen()

        page.addView(ui.eyebrow(c.t("tab_settings")), ui.lp(t = 8))
        page.addView(ui.h1(c.t("settings_title")), ui.lp(t = 6, b = 4))

        // ------------------------------------------------------------ appearance
        page.addView(ui.sectionTitle(c.t("appearance")))
        val look = ui.card(16)
        look.addView(ui.text(c.t("theme"), 15f, p.ink, Fonts.sansMedium), ui.lp(b = 12))
        val rows = listOf(Themes.all.take(4), Themes.all.drop(4))
        for (group in rows) {
            val r = ui.row(Gravity.TOP)
            for (t in group) {
                val sel = t.id == prefs.themeId
                val tp = t.pal(prefs.dark)
                val cell = ui.col {
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(ui.dp(2), ui.dp(6), ui.dp(2), ui.dp(6))
                    val sw = View(act).apply {
                        background = GlowDrawable(tp, ui.dpf(26), act.resources.displayMetrics.density, t.id.hashCode())
                    }
                    val ring = android.widget.FrameLayout(act).apply {
                        setPadding(ui.dp(3), ui.dp(3), ui.dp(3), ui.dp(3))
                        background = oval(0, if (sel) p.accent else p.line, ui.dp(if (sel) 3 else 1))
                        addView(sw, android.widget.FrameLayout.LayoutParams(MATCH, MATCH))
                        val dot = View(act).apply { background = oval(tp.accent, tp.surface, ui.dp(2)) }
                        addView(dot, android.widget.FrameLayout.LayoutParams(ui.dp(18), ui.dp(18), Gravity.CENTER))
                        sw.clipToOutline = true
                        sw.outlineProvider = object : android.view.ViewOutlineProvider() {
                            override fun getOutline(view: View, outline: android.graphics.Outline) {
                                outline.setOval(0, 0, view.width, view.height)
                            }
                        }
                    }
                    addView(ring, ui.lp(ui.dp(54), ui.dp(54), b = 6))
                    addView(ui.text(c.t(t.nameKey), 11.5f, if (sel) p.ink else p.muted, if (sel) Fonts.sansBold else Fonts.sans, 1.15f, Gravity.CENTER))
                }
                ui.clickable(cell, ui.dpf(16)) {
                    prefs.themeId = t.id
                    act.applyTheme()
                }
                r.addView(cell, ui.lp(0, WRAP, 1f))
            }
            if (group.size < 4) repeat(4 - group.size) { r.addView(View(act), ui.lp(0, 1, 1f)) }
            look.addView(r, ui.lp(b = 4))
        }
        look.addView(ui.divider(), ui.lp(t = 10, b = 4))
        look.addView(switchRow(ui, c.t("night_mode"), null, prefs.dark) {
            prefs.dark = it
            act.applyTheme()
        })
        page.addView(look, ui.lp())

        // ------------------------------------------------------------ language & voice
        page.addView(ui.sectionTitle(c.t("voice")))
        val voice = ui.card(16)
        voice.addView(valueRow(ui, c.t("language"), Content.NATIVE[prefs.lang] ?: prefs.lang, c.t("language_note")) { pickLanguage() })
        voice.addView(ui.divider(), ui.lp(t = 4, b = 12))
        voice.addView(ui.text(c.t("voice"), 15f, p.ink, Fonts.sansMedium), ui.lp(b = 10))
        voice.addView(segmented(ui, listOf("female" to c.t("voice_female"), "male" to c.t("voice_male")), prefs.voiceGender) {
            prefs.voiceGender = it
            prefs.setVoiceName(prefs.lang, null)
            act.speaker.invalidateConfig()
            act.refresh()
            act.speaker.sample(prefs, c.t("voice_sample"))
        }, ui.lp(b = 8))
        val specificName = prefs.voiceName(prefs.lang)
        val specificLabel = if (specificName == null) c.t("voice_auto")
        else act.speaker.offlineVoices(prefs.lang).firstOrNull { it.voice.name == specificName }?.label ?: specificName
        voice.addView(valueRow(ui, c.t("voice_specific"), specificLabel, null) { pickVoice() })
        voice.addView(ui.divider(), ui.lp(t = 4, b = 12))

        voice.addView(ui.row {
            addView(ui.text(c.t("speech_rate"), 15f, p.ink, Fonts.sansMedium), ui.lp(0, WRAP, 1f))
            addView(ui.muted(String.format(java.util.Locale.US, "%.2f×", prefs.speechRate), 13f).also { it.tag = "rate" })
        })
        val rateLabel = (voice.getChildAt(voice.childCount - 1) as LinearLayout).findViewWithTag<TextView>("rate")
        val seek = SeekBar(act).apply {
            max = 100
            progress = ((prefs.speechRate - 0.5f) * 100).toInt().coerceIn(0, 100)
            progressTintList = ColorStateList.valueOf(p.accent)
            thumbTintList = ColorStateList.valueOf(p.accent)
            progressBackgroundTintList = ColorStateList.valueOf(p.line)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    val rate = 0.5f + value / 100f
                    rateLabel?.text = String.format(java.util.Locale.US, "%.2f×", rate)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {
                    prefs.speechRate = 0.5f + (sb?.progress ?: 50) / 100f
                    act.speaker.invalidateConfig()
                }
            })
        }
        voice.addView(seek, ui.lp(t = 6, b = 12))
        voice.addView(ui.row {
            addView(ui.button(c.t("test_voice"), Btn.SECONDARY, Icon.SPEAKER) { act.speaker.sample(prefs, c.t("voice_sample")) }, ui.lp(0, WRAP, 1f, r = 8))
            addView(ui.button(c.t("install_voices"), Btn.GHOST) { openVoiceInstaller(act) }, ui.lp(0, WRAP, 1f))
        })
        page.addView(voice, ui.lp())

        // ------------------------------------------------------------ reminders
        page.addView(ui.sectionTitle(c.t("reminders")))
        val rem = ui.card(16)
        rem.addView(switchRow(ui, c.t("reminders_enabled"), null, prefs.remindersOn) {
            prefs.remindersOn = it
            if (it) act.requestNotificationsIfNeeded()
            Reminders.reschedule(act)
            act.refresh()
        })
        if (prefs.remindersOn) {
            rem.addView(ui.divider(), ui.lp(t = 4, b = 4))
            rem.addView(valueRow(ui, c.t("reminder_start"), ui.minutesLabel(prefs.reminderStartMin), null) {
                pickTime(prefs.reminderStartMin) { m ->
                    prefs.reminderStartMin = m
                    if (prefs.reminderEndMin < m) prefs.reminderEndMin = m
                    Journey.realignUnlock(prefs)
                }
            })
            rem.addView(valueRow(ui, c.t("reminder_end"), ui.minutesLabel(prefs.reminderEndMin), null) {
                pickTime(prefs.reminderEndMin) { m -> prefs.reminderEndMin = maxOf(m, prefs.reminderStartMin) }
            })
            rem.addView(ui.text(c.t("reminder_interval"), 15f, p.ink, Fonts.sansMedium), ui.lp(t = 10, b = 10))
            val intervals = listOf(60, 120, 180, 240, 360)
            rem.addView(segmented(ui, intervals.map { it.toString() to c.t("hours_n", "n" to it / 60) }, prefs.reminderIntervalMin.toString()) {
                prefs.reminderIntervalMin = it.toInt()
                Reminders.reschedule(act)
                act.refresh()
            }, ui.lp(b = 8))
            if (Build.VERSION.SDK_INT >= 31) {
                val exact = Reminders.canExact(act)
                rem.addView(valueRow(ui, c.t("precise_reminders"), if (exact) c.t("precise_on") else c.t("precise_off"), null) {
                    try {
                        act.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + act.packageName)))
                    } catch (e: Exception) {
                        act.toast(c.t("precise_off"))
                    }
                })
            }
            rem.addView(ui.text(c.t("unlock_mode"), 15f, p.ink, Fonts.sansMedium), ui.lp(t = 10, b = 10))
            rem.addView(segmented(ui, listOf("next_day" to c.t("unlock_next_day"), "immediate" to c.t("unlock_immediately")), prefs.unlockMode) {
                prefs.unlockMode = it
                if (it == "immediate") prefs.unlockAt = 0L
                Reminders.reschedule(act)
                act.refresh()
            })
        }
        page.addView(rem, ui.lp())

        // ------------------------------------------------------------ journey
        page.addView(ui.sectionTitle(c.t("journey_section")))
        val j = ui.card(16)
        j.addView(ui.muted(c.t("reset_journey_body"), 13.5f), ui.lp(b = 12))
        j.addView(ui.button(c.t("reset_journey"), Btn.DANGER) {
            act.confirm(c.t("reset_journey_body"), c.t("reset_journey"), danger = true, title = c.t("reset_journey")) {
                Journey.reset(act)
                act.toast(c.t("reset_done"))
                act.refresh()
            }
        }, ui.lp())
        page.addView(j, ui.lp())

        // ------------------------------------------------------------ about
        page.addView(ui.sectionTitle(c.t("about")))
        val about = ui.card(16)
        val version = try {
            @Suppress("DEPRECATION")
            act.packageManager.getPackageInfo(act.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) { "1.0" }
        about.addView(ui.row {
            addView(ui.text(c.t("app_name"), 16f, p.ink, Fonts.serifBold), ui.lp(0, WRAP, 1f))
            addView(ui.muted(c.t("version") + " " + version, 13f))
        })
        about.addView(ui.muted(c.t("content_note"), 13f), ui.lp(t = 6))
        val credit = ui.row {
            addView(IconView(act, Icon.HEART, p.accent, 16), ui.lp(ui.dp(16), ui.dp(16), r = 8))
            addView(ui.text("Created with Love by ", 13.5f, p.muted))
            addView(ui.text("sj.pixels", 13.5f, p.accent, Fonts.sansBold).apply { paint.isUnderlineText = true })
        }
        ui.clickable(credit, ui.dpf(12)) { act.openUrl(MainActivity.CREDIT_URL) }
        about.addView(credit, ui.lp(t = 12))
        page.addView(about, ui.lp())

        return ui.page(page)
    }

    // ------------------------------------------------------------ rows

    private fun switchRow(ui: Ui, label: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit): View {
        val p = ui.p
        return ui.row {
            setPadding(0, ui.dp(6), 0, ui.dp(6))
            addView(ui.col {
                addView(ui.text(label, 15f, p.ink, Fonts.sansMedium))
                if (sub != null) addView(ui.muted(sub, 12.5f), ui.lp(t = 2))
            }, ui.lp(0, WRAP, 1f))
            val sw = Switch(act).apply {
                isChecked = checked
                val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
                thumbTintList = ColorStateList(states, intArrayOf(p.accent, if (p.dark) p.muted else p.surface))
                trackTintList = ColorStateList(states, intArrayOf(withAlpha(p.accent, 120), withAlpha(p.ink, 60)))
                setOnCheckedChangeListener { _, v -> onChange(v) }
            }
            addView(sw)
        }
    }

    private fun valueRow(ui: Ui, label: String, value: String, sub: String?, onClick: () -> Unit): View {
        val p = ui.p
        val r = ui.row {
            setPadding(ui.dp(4), ui.dp(10), ui.dp(4), ui.dp(10))
            addView(ui.col {
                addView(ui.text(label, 15f, p.ink, Fonts.sansMedium))
                if (sub != null) addView(ui.muted(sub, 12.5f), ui.lp(t = 2))
            }, ui.lp(0, WRAP, 1f, r = 10))
            addView(ui.text(value, 14f, p.accent, Fonts.sansMedium, 1.2f, Gravity.END), ui.lp(0, WRAP, 0.8f))
            addView(IconView(act, Icon.CHEVRON, p.muted, 16), ui.lp(ui.dp(16), ui.dp(16), l = 6))
        }
        ui.clickable(r, ui.dpf(12), onClick)
        return r
    }

    private fun segmented(ui: Ui, options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit): View {
        val p = ui.p
        val bar = ui.row {
            setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4))
            background = rounded(p.surfaceAlt, ui.dpf(22), p.line, ui.dp(1))
        }
        for ((value, label) in options) {
            val sel = value == selected
            val t = ui.text(label, 13.5f, if (sel) p.onAccent else p.ink, if (sel) Fonts.sansBold else Fonts.sansMedium, 1.1f, Gravity.CENTER).apply {
                setPadding(ui.dp(6), ui.dp(9), ui.dp(6), ui.dp(9))
                background = rounded(if (sel) p.accent else 0, ui.dpf(18))
                maxLines = 2
            }
            if (!sel) ui.clickable(t, ui.dpf(18)) { onPick(value) }
            bar.addView(t, ui.lp(0, WRAP, 1f))
        }
        return bar
    }

    // ------------------------------------------------------------ dialogs

    private fun pickLanguage() {
        val prefs = act.prefs
        val langs = Content.LANGS
        val names = langs.map { Content.NATIVE[it] ?: it }.toTypedArray()
        val d = act.dialogBuilder()
            .setTitle(act.pack.t("language"))
            .setSingleChoiceItems(names, langs.indexOf(prefs.lang).coerceAtLeast(0)) { dlg, which ->
                dlg.dismiss()
                if (langs[which] != prefs.lang) {
                    act.speaker.stop()
                    prefs.lang = langs[which]
                    act.applyTheme()
                }
            }
            .setNegativeButton(act.pack.t("cancel"), null)
            .create()
        d.setOnShowListener { act.styleDialog(d) }
        d.show()
    }

    private fun pickVoice() {
        val prefs = act.prefs
        val c = act.pack
        val voices = act.speaker.offlineVoices(prefs.lang)
        if (voices.isEmpty()) {
            act.confirm(c.t("voice_none"), c.t("install_voices")) { openVoiceInstaller(act) }
            return
        }
        val labels = (listOf(c.t("voice_auto")) + voices.map { v ->
            val g = when (v.gender) {
                "female" -> " · " + c.t("voice_female")
                "male" -> " · " + c.t("voice_male")
                else -> ""
            }
            v.label + g
        }).toTypedArray()
        val currentName = prefs.voiceName(prefs.lang)
        val checked = if (currentName == null) 0 else (voices.indexOfFirst { it.voice.name == currentName } + 1).coerceAtLeast(0)
        var choice = checked
        val d = act.dialogBuilder()
            .setTitle(c.t("voice_specific"))
            .setSingleChoiceItems(labels, checked) { _, which ->
                choice = which
                if (which > 0) act.speaker.sample(prefs, c.t("voice_sample"), voices[which - 1].voice)
            }
            .setPositiveButton(c.t("continue")) { _, _ ->
                prefs.setVoiceName(prefs.lang, if (choice == 0) null else voices[choice - 1].voice.name)
                val g = if (choice > 0) voices[choice - 1].gender else null
                if (g != null) prefs.voiceGender = g
                act.speaker.invalidateConfig()
                act.refresh()
            }
            .setNegativeButton(c.t("cancel")) { _, _ -> act.speaker.invalidateConfig() }
            .create()
        d.setOnShowListener { act.styleDialog(d) }
        d.show()
    }

    private fun pickTime(current: Int, onSet: (Int) -> Unit) {
        val theme = if (act.pal.dark) android.R.style.Theme_Material_Dialog_Alert else android.R.style.Theme_Material_Light_Dialog_Alert
        val d = TimePickerDialog(act, theme, { _, h, m ->
            onSet(h * 60 + m)
            Reminders.reschedule(act)
            act.refresh()
        }, current / 60, current % 60, DateFormat.is24HourFormat(act))
        d.setOnShowListener { act.styleDialog(d) }
        d.show()
    }
}
