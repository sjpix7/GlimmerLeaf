package com.sjpixels.glimmerleaf

import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class SettingsScreen(act: MainActivity) : Screen(act) {

    override fun build(ui: Ui): View {
        val c = ui.c
        val p = ui.p
        val prefs = act.prefs
        val page = ui.screen()

        page.addView(ui.eyebrow(c.t("tab_settings")), ui.lp(t = 8))
        page.addView(ui.h1(c.t("settings_title")), ui.lp(t = 4, b = 4))

        // ------------------------------------------------------------ appearance
        page.addView(ui.sectionTitle(c.t("appearance")))
        val look = ui.card(16)
        look.addView(ui.h3(c.t("theme")), ui.lp(b = 8))
        // Seven swatches in rows of 4 and 3 (2 per row at very large font sizes). Every cell has the
        // same layout, so circles and names line up; the selection is a ring around the circle only.
        val perRow = if (act.resources.configuration.fontScale > 1.3f) 2 else 4
        for (group in Themes.all.chunked(perRow)) {
            val r = ui.row(Gravity.TOP)
            for (t in group) {
                val sel = t.id == prefs.themeId
                val tp = t.pal(prefs.dark)
                val cell = ui.col {
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(ui.dp(2), ui.dp(4), ui.dp(2), ui.dp(8))
                    val ring = android.widget.FrameLayout(act).apply {
                        background = oval(0, if (sel) p.accent else 0, ui.dp(2))
                        val dot = View(act).apply { background = oval(tp.accent, tp.line, ui.dp(1)) }
                        addView(dot, android.widget.FrameLayout.LayoutParams(ui.dp(32), ui.dp(32), Gravity.CENTER))
                    }
                    addView(ring, ui.lp(ui.dp(44), ui.dp(44), b = 4))
                    addView(ui.text(c.t(t.nameKey), 12f, if (sel) p.ink else p.muted, if (sel) Fonts.sansMedium else Fonts.sans,
                        gravity = Gravity.CENTER, lineSp = 16f).apply { maxLines = 2 }, ui.lp(MATCH, WRAP))
                    contentDescription = c.t(t.nameKey)
                    isSelected = sel
                }
                ui.clickable(cell, ui.dpf(12)) {
                    prefs.themeId = t.id
                    act.applyTheme()
                }
                r.addView(cell, ui.lp(0, WRAP, 1f))
            }
            repeat(perRow - group.size) { r.addView(View(act), ui.lp(0, 1, 1f)) }
            look.addView(r, ui.lp())
        }
        look.addView(ui.divider(), ui.lp(t = 8, b = 4))
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
        voice.addView(ui.h3(c.t("voice")), ui.lp(b = 8))
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
            addView(ui.h3(c.t("speech_rate")), ui.lp(0, WRAP, 1f))
            addView(ui.text(String.format(java.util.Locale.US, "%.2f×", prefs.speechRate), 16f, p.ink, Fonts.sansMedium, lineSp = 24f).also { it.tag = "rate" })
        })
        val rateLabel = (voice.getChildAt(voice.childCount - 1) as LinearLayout).findViewWithTag<TextView>("rate")
        // Slider: 0.50× to 1.50× in steps of 0.05×; accent active track and thumb, muted inactive line.
        val seek = SeekBar(act).apply {
            max = 20
            progress = Math.round((prefs.speechRate - 0.5f) * 20).coerceIn(0, 20)
            progressTintList = ColorStateList.valueOf(p.accent)
            thumbTintList = ColorStateList.valueOf(p.accent)
            progressBackgroundTintList = ColorStateList.valueOf(p.muted)
            minimumHeight = ui.dp(48)
            contentDescription = c.t("speech_rate")
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    val label = String.format(java.util.Locale.US, "%.2f×", 0.5f + value * 0.05f)
                    rateLabel?.text = label
                    sb?.stateDescriptionCompat(label)
                    if (fromUser && sb?.isPressed != true) {
                        // Keyboard and accessibility adjustments save straight away.
                        prefs.speechRate = 0.5f + value * 0.05f
                        act.speaker.invalidateConfig()
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {
                    prefs.speechRate = 0.5f + (sb?.progress ?: 10) * 0.05f
                    act.speaker.invalidateConfig()
                }
            })
        }
        voice.addView(seek, ui.lp(t = 4, b = 12))
        voice.addView(ui.button(c.t("test_voice"), Btn.SECONDARY, Icon.SPEAKER) { act.speaker.sample(prefs, c.t("voice_sample")) }, ui.lp())
        voice.addView(ui.button(c.t("install_voices"), Btn.GHOST) { openVoiceInstaller(act) }, ui.lp(t = 8))
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
            rem.addView(ui.h3(c.t("reminder_interval")), ui.lp(t = 12, b = 8))
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
            rem.addView(ui.h3(c.t("unlock_mode")), ui.lp(t = 12, b = 8))
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
        j.addView(ui.muted(c.t("reset_journey_body")), ui.lp(b = 12))
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
            addView(ImageView(act).apply {
                setImageResource(R.drawable.brand_mark)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, ui.lp(ui.dp(40), ui.dp(40), r = 12))
            addView(ui.col {
                addView(ui.cardTitle(c.t("app_name")))
                addView(ui.muted(c.t("version") + " " + version))
            }, ui.lp(0, WRAP, 1f))
        })
        about.addView(ui.muted(c.t("content_note")), ui.lp(t = 12))
        val credit = ui.row {
            minimumHeight = ui.dp(48)
            addView(IconView(act, Icon.HEART, p.accent, 20), ui.lp(ui.dp(20), ui.dp(20), r = 8))
            addView(ui.muted("Created with Love by "))
            addView(ui.text("sj.pixels", 14f, p.ink, Fonts.sansMedium, lineSp = 20f).apply { paint.isUnderlineText = true })
        }
        ui.clickable(credit, ui.dpf(12)) { act.openUrl(MainActivity.CREDIT_URL) }
        about.addView(credit, ui.lp(t = 4))
        page.addView(about, ui.lp())

        return ui.page(page)
    }

    // ------------------------------------------------------------ rows

    private fun switchRow(ui: Ui, label: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit): View {
        val p = ui.p
        return ui.row {
            minimumHeight = ui.dp(48)
            val texts = ui.col {
                addView(ui.h3(label))
                if (sub != null) addView(ui.muted(sub), ui.lp(t = 2))
            }
            addView(texts, ui.lp(0, WRAP, 1f, r = 12))
            val sw = KitSwitch(act, p, checked, onChange = onChange).apply { contentDescription = label }
            addView(sw)
            // The whole row toggles the switch.
            setOnClickListener { sw.performClick() }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    private fun valueRow(ui: Ui, label: String, value: String, sub: String?, onClick: () -> Unit): View {
        val p = ui.p
        val r = ui.row {
            minimumHeight = ui.dp(48)
            setPadding(ui.dp(4), ui.dp(8), ui.dp(4), ui.dp(8))
            addView(ui.col {
                addView(ui.h3(label))
                if (sub != null) addView(ui.muted(sub), ui.lp(t = 2))
            }, ui.lp(0, WRAP, 1f, r = 12))
            addView(ui.text(value, 14f, p.ink, Fonts.sans, gravity = Gravity.END, lineSp = 20f), ui.lp(0, WRAP, 0.8f))
            addView(IconView(act, Icon.CHEVRON, p.muted, 20), ui.lp(ui.dp(20), ui.dp(20), l = 8))
        }
        ui.clickable(r, ui.dpf(12), onClick)
        return r
    }

    // ------------------------------------------------------------ dialogs

    private fun pickLanguage() {
        val prefs = act.prefs
        val langs = Content.LANGS
        val names = langs.map { Content.NATIVE[it] ?: it }.toTypedArray()
        val d = act.dialogBuilder()
            .setTitle(act.pack.t("language"))
            .setSingleChoiceItems(act.choiceAdapter(names), langs.indexOf(prefs.lang).coerceAtLeast(0)) { dlg, which ->
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
            .setSingleChoiceItems(act.choiceAdapter(labels), checked) { _, which ->
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

private fun View.stateDescriptionCompat(text: String) {
    if (Build.VERSION.SDK_INT >= 30) stateDescription = text
}
