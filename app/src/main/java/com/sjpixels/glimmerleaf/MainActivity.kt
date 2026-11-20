package com.sjpixels.glimmerleaf

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

/** A page of the app. Screens are rebuilt from fresh data whenever they are shown. */
abstract class Screen(val act: MainActivity) {
    private var savedScroll = 0
    protected var scroller: ScrollView? = null

    abstract fun build(ui: Ui): View

    fun view(ui: Ui): View {
        scroller = null
        val v = build(ui)
        val s = scroller
        if (s != null && savedScroll > 0) {
            val y = savedScroll
            s.post { s.scrollTo(0, y) }
        }
        return v
    }

    /** Called before the view is replaced (rebuild or navigation). */
    open fun onHide() {
        scroller?.let { savedScroll = it.scrollY }
    }

    /** Called when the screen leaves the display for another screen. */
    open fun onLeave() {}

    open val refreshOnResume: Boolean = true

    protected fun Ui.page(content: LinearLayout): ScrollView {
        content.addView(space(28))
        return scroll(content).also { scroller = it }
    }
}

class MainActivity : Activity() {

    companion object {
        const val EXTRA_OPEN_DAY = "open_day"
        const val CREDIT_URL = "https://sjpixels.com/"
        private const val REQ_NOTIF = 11
    }

    lateinit var prefs: Prefs
    lateinit var pal: Pal
    lateinit var pack: ContentPack
    lateinit var speaker: Speaker

    private lateinit var root: FrameLayout
    private lateinit var column: LinearLayout
    private lateinit var host: FrameLayout
    private lateinit var nav: View
    private lateinit var navRow: LinearLayout
    private lateinit var leftSlot: FrameLayout
    private var overlay: View? = null

    private var tab = 0
    private val stack = ArrayList<Screen>()
    private val tabScreens = HashMap<Int, Screen>()
    private var current: Screen? = null
    /** Holds an OnBackInvokedCallback on Android 13+ (typed loosely so older versions never load the class). */
    private var backCallback: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        loadTheme()
        speaker = Speaker(this)
        Notifier.ensureChannel(this)
        buildShell()
        render()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val day = i?.getIntExtra(EXTRA_OPEN_DAY, 0) ?: 0
        if (day in 1..Content.DAYS) {
            i?.removeExtra(EXTRA_OPEN_DAY)
            dismissOverlay()
            stack.clear()
            tab = 0
            openPractice(day)
        }
    }

    private var firstResume = true

    override fun onResume() {
        super.onResume()
        Reminders.reschedule(this)
        if (firstResume) {
            firstResume = false
            return
        }
        if (current?.refreshOnResume == true && overlay == null) refresh()
    }

    override fun onPause() {
        super.onPause()
        current?.onHide()
    }

    override fun onDestroy() {
        speaker.shutdown()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Follows the system dark setting until the user picks day or night with the toggle.
        if (::host.isInitialized) applyTheme()
    }

    fun ui(): Ui = Ui(this, pal, pack)

    private fun loadTheme() {
        pal = Themes.get(prefs.themeId).pal(prefs.dark)
        pack = Content.get(this, prefs.lang)
    }

    // ------------------------------------------------------------------ shell

    private fun buildShell() {
        val ui = ui()
        root = FrameLayout(this)
        root.background = BackdropDrawable(pal)
        column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(column, FrameLayout.LayoutParams(MATCH, MATCH))

        column.addView(buildTopBar(ui), LinearLayout.LayoutParams(MATCH, WRAP))
        host = FrameLayout(this)
        column.addView(host, LinearLayout.LayoutParams(MATCH, 0, 1f))
        nav = buildNav(ui)
        column.addView(nav, LinearLayout.LayoutParams(MATCH, WRAP))

        setContentView(root)
        applySystemBars()
    }

    private fun buildTopBar(ui: Ui): LinearLayout {
        // Top bar: 56dp minimum; 48dp brand/Up slot, centred credit, 56dp day/night control.
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(56)
            setPadding(dp(8), 0, dp(8), 0)
        }
        leftSlot = FrameLayout(this)
        bar.addView(leftSlot, LinearLayout.LayoutParams(dp(48), dp(48)))

        val credit = TextView(this).apply {
            val prefix = "Created with Love by "
            val brand = "sj.pixels"
            val s = SpannableString(prefix + brand)
            s.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) = openUrl(CREDIT_URL)
                override fun updateDrawState(ds: TextPaint) {
                    ds.color = pal.ink
                    ds.isUnderlineText = true
                }
            }, prefix.length, prefix.length + brand.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text = s
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
            textSize = 12f
            setTextColor(pal.muted)
            typeface = Fonts.sans
            gravity = Gravity.CENTER
            minHeight = dp(48)
            setPadding(dp(4), 0, dp(4), 0)
        }
        bar.addView(credit, LinearLayout.LayoutParams(0, WRAP, 1f))

        val toggle = DayNightToggle(this, pal, prefs.dark) { dark ->
            prefs.dark = dark
            applyTheme()
        }
        toggle.contentDescription = pack.t("night_mode")
        bar.addView(toggle, LinearLayout.LayoutParams(WRAP, WRAP))
        return bar
    }

    private fun updateLeftSlot() {
        val ui = ui()
        leftSlot.removeAllViews()
        if (stack.isNotEmpty()) {
            val b = ui.iconButton(Icon.BACK, pack.t("close"), pal.ink, 48, 24) { handleBack() }
            leftSlot.addView(b, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER))
        } else {
            val mark = android.widget.ImageView(this).apply {
                setImageResource(R.drawable.brand_mark)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            leftSlot.addView(mark, FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER))
        }
    }

    private data class NavItem(val icon: Icon, val key: String)

    private val navItems = listOf(
        NavItem(Icon.HOME, "tab_today"),
        NavItem(Icon.BOOK, "tab_contents"),
        NavItem(Icon.HISTORY, "tab_history"),
        NavItem(Icon.CHART, "tab_insights"),
        NavItem(Icon.GEAR, "tab_settings"),
    )

    /**
     * Bottom nav: 64dp minimum on surface with a top hairline. 24dp icons, 4dp gap, 11sp labels;
     * the selected tab has a 48×32 accentSoft pill and an ink icon. Tabs share the width equally;
     * if a label (for example "Einstellungen") doesn't fit, the strip scrolls sideways instead of clipping.
     */
    private fun buildNav(ui: Ui): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }
        navRow = row
        navItems.forEachIndexed { i, item ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                minimumHeight = dp(52)
            }
            val pill = FrameLayout(this)
            val icon = IconView(this, item.icon, pal.muted, 24)
            pill.addView(icon, FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER))
            cell.addView(pill, LinearLayout.LayoutParams(dp(48), dp(32)))
            val label = ui.text(pack.t(item.key), 11f, pal.muted, Fonts.sansMedium, gravity = Gravity.CENTER, lineSp = 16f).apply {
                maxLines = 1
                setPadding(dp(8), 0, dp(8), 0)
            }
            cell.addView(label, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(4) })
            cell.background = ui.ripple(null, rounded(Color.WHITE, dpf(16)))
            cell.isClickable = true
            cell.isFocusable = true
            cell.contentDescription = pack.t(item.key)
            cell.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                selectTab(i)
            }
            cell.tag = Triple(pill, icon, label)
            row.addView(cell, LinearLayout.LayoutParams(WRAP, WRAP, 1f))
        }
        return android.widget.HorizontalScrollView(this).apply {
            isFillViewport = true
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            background = android.graphics.drawable.LayerDrawable(
                arrayOf(
                    android.graphics.drawable.ColorDrawable(pal.surface),
                    android.graphics.drawable.ColorDrawable(pal.line),
                )
            ).apply { setLayerHeight(1, dp(1)) }
            minimumHeight = dp(64)
            addView(row, FrameLayout.LayoutParams(MATCH, WRAP))
        }
    }

    private fun updateNav() {
        for (i in 0 until navRow.childCount) {
            val cell = navRow.getChildAt(i)
            @Suppress("UNCHECKED_CAST")
            val parts = cell.tag as Triple<FrameLayout, IconView, TextView>
            val (pill, icon, label) = parts
            val sel = i == tab
            pill.background = if (sel) rounded(pal.accentSoft, dpf(16)) else null
            icon.color = if (sel) pal.ink else pal.muted
            label.setTextColor(if (sel) pal.ink else pal.muted)
            cell.isSelected = sel
            if (sel) (nav as? android.widget.HorizontalScrollView)?.post {
                (nav as android.widget.HorizontalScrollView).requestChildRectangleOnScreen(navRow,
                    android.graphics.Rect(cell.left, 0, cell.right, cell.height), false)
            }
        }
    }

    private fun applySystemBars() {
        val w = window
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false)
            if (Build.VERSION.SDK_INT < 35) {
                @Suppress("DEPRECATION")
                w.statusBarColor = Color.TRANSPARENT
                @Suppress("DEPRECATION")
                w.navigationBarColor = Color.TRANSPARENT
            }
            w.isNavigationBarContrastEnforced = false
            val light = if (pal.dark) 0 else
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            w.insetsController?.setSystemBarsAppearance(
                light,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
            root.setOnApplyWindowInsetsListener { _, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val ime = insets.getInsets(WindowInsets.Type.ime())
                val imeOpen = ime.bottom > bars.bottom
                nav.visibility = if (imeOpen) View.GONE else View.VISIBLE
                column.setPadding(bars.left, bars.top, bars.right, if (imeOpen) ime.bottom else bars.bottom)
                insets
            }
        } else {
            @Suppress("DEPRECATION")
            run {
                w.statusBarColor = pal.bg
                w.navigationBarColor = pal.surface
                var flags = w.decorView.systemUiVisibility
                flags = if (!pal.dark) flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                else flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                w.decorView.systemUiVisibility = flags
            }
        }
    }

    // ------------------------------------------------------------------ navigation

    private fun tabScreen(i: Int): Screen = tabScreens.getOrPut(i) {
        when (i) {
            0 -> HomeScreen(this)
            1 -> ContentsScreen(this)
            2 -> HistoryScreen(this)
            3 -> InsightsScreen(this)
            else -> SettingsScreen(this)
        }
    }

    private fun render() {
        val s = stack.lastOrNull() ?: tabScreen(tab)
        current?.let { cur ->
            cur.onHide()
            if (cur !== s) cur.onLeave()
        }
        current = s
        hideKeyboard()
        host.removeAllViews()
        host.addView(s.view(ui()), FrameLayout.LayoutParams(MATCH, MATCH))
        updateLeftSlot()
        updateNav()
        updateBackHandling()
    }

    /** Rebuilds the current screen with fresh data. */
    fun refresh() {
        if (!::host.isInitialized) return
        render()
    }

    /** Re-applies theme, night mode and language everywhere. */
    fun applyTheme() {
        loadTheme()
        speaker.invalidateConfig()
        Notifier.ensureChannel(this)
        buildShell()
        render()
    }

    fun selectTab(i: Int) {
        dismissOverlay()
        stack.clear()
        tab = i
        render()
    }

    fun push(s: Screen) {
        stack += s
        render()
    }

    fun openPractice(day: Int) = push(PracticeScreen(this, day))

    /** Which list the History tab shows: 0 journeys, 1 daily gratitude, 2 journal. */
    var historySection = 0

    fun openHistory(section: Int) {
        historySection = section
        selectTab(2)
    }

    fun openRitual(id: String) = push(RitualScreen(this, id))

    /** Leaves the current inner page (used after saving or deleting on an edit page). */
    fun goBack() { handleBack() }

    private fun handleBack(): Boolean {
        if (overlay != null) { dismissOverlay(); return true }
        if (stack.isNotEmpty()) {
            stack.removeAt(stack.size - 1)
            render()
            return true
        }
        if (tab != 0) { selectTab(0); return true }
        return false
    }

    private fun updateBackHandling() {
        if (Build.VERSION.SDK_INT < 33) return
        val need = overlay != null || stack.isNotEmpty() || tab != 0
        val cb = backCallback as? OnBackInvokedCallback
        if (need && cb == null) {
            val n = OnBackInvokedCallback { handleBack() }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, n)
            backCallback = n
        } else if (!need && cb != null) {
            onBackInvokedDispatcher.unregisterOnBackInvokedCallback(cb)
            backCallback = null
        }
    }

    /** Fallback for Android 12 and older. On 13+ back (including the predictive gesture) goes through OnBackInvokedCallback above. */
    @SuppressLint("GestureBackNavigation")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!handleBack()) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    // ------------------------------------------------------------------ helpers

    fun dp(v: Number) = (this as android.content.Context).dp(v)
    fun dpf(v: Number) = (this as android.content.Context).dpf(v)

    fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            toast(url)
        }
    }

    fun hideKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java) ?: return
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    fun dialogBuilder(): AlertDialog.Builder = AlertDialog.Builder(
        this,
        if (pal.dark) android.R.style.Theme_Material_Dialog_Alert else android.R.style.Theme_Material_Light_Dialog_Alert
    )

    fun styleDialog(d: AlertDialog) {
        d.window?.setBackgroundDrawable(rounded(pal.surface, dpf(20)))
        for (b in intArrayOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL)) {
            d.getButton(b)?.setTextColor(pal.accent)
        }
    }

    /** Single-choice rows whose radio buttons use the theme accent instead of the platform default. */
    fun choiceAdapter(items: Array<String>): android.widget.ArrayAdapter<String> =
        object : android.widget.ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, items) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as? android.widget.CheckedTextView)?.apply {
                    layoutParams = android.widget.AbsListView.LayoutParams(MATCH, WRAP)
                    if (getTag(R.id.choice_styled) == null) {
                        setTag(R.id.choice_styled, true)
                        // Radio at the start, like the platform list, tinted with the theme accent.
                        val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.listChoiceIndicatorSingle))
                        val radio = a.getDrawable(0)?.mutate()
                        a.recycle()
                        radio?.setTintList(android.content.res.ColorStateList.valueOf(pal.accent))
                        checkMarkDrawable = null
                        setCompoundDrawablesRelativeWithIntrinsicBounds(radio, null, null, null)
                        compoundDrawablePadding = dp(20)
                        setPaddingRelative(dp(20), dp(8), dp(20), dp(8))
                        setTextColor(pal.ink)
                        textSize = 16f
                        minHeight = dp(48)
                    }
                }
                return v
            }
        }

    fun confirm(message: String, positive: String, danger: Boolean = false, title: String? = null, onYes: () -> Unit) {
        val b = dialogBuilder().setMessage(message)
            .setPositiveButton(positive) { _, _ -> onYes() }
            .setNegativeButton(pack.t("cancel"), null)
        if (title != null) b.setTitle(title)
        val d = b.create()
        d.setOnShowListener {
            styleDialog(d)
            if (danger) d.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(pal.danger)
        }
        d.show()
    }

    fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            prefs.askedNotifications = true
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIF) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                toast(pack.t("notif_permission"))
            }
            Reminders.reschedule(this)
        }
    }

    fun startJourney() {
        Journey.start(this)
        requestNotificationsIfNeeded()
    }

    // ------------------------------------------------------------------ celebration

    fun celebrate(result: CompleteResult) {
        dismissOverlay()
        val ui = ui()
        val d = pack.day(result.day)
        val words = pack.list("celebrate_words").ifEmpty { listOf("Hooray!") }
        val title = if (result.cycleCompleted) words.first() else words.random()
        val body = when {
            result.cycleCompleted -> pack.t("celebrate_cycle")
            result.firstTime -> pack.t("celebrate_body", "n" to result.day, "title" to d.title)
            else -> pack.t("celebrate_again", "n" to result.day)
        }
        val layer = FrameLayout(this).apply {
            setBackgroundColor(withAlpha(Color.BLACK, 133))
            isClickable = true
            alpha = 0f
        }
        val big = result.cycleCompleted
        val card = ui.col(20, 24).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            background = GlowDrawable(pal, dpf(24), resources.displayMetrics.density, 11)
            elevation = dpf(8)
            clipToOutline = true
        }
        card.addView(IconView(this, if (big) Icon.HEART else Icon.SPARK, pal.accent, 40), ui.lp(dp(40), dp(40), b = 12))
        card.addView(ui.text(title, if (big) 32f else 28f, pal.ink, Fonts.serifBold, gravity = Gravity.CENTER, lineSp = if (big) 40f else 36f), ui.lp(MATCH, WRAP, b = 8))
        card.addView(ui.text(body, 16f, pal.ink, Fonts.sans, gravity = Gravity.CENTER, lineSp = 24f), ui.lp(MATCH, WRAP, b = 8))
        card.addView(ui.text(d.affirmation, 17f, pal.muted, Fonts.serifItalic, gravity = Gravity.CENTER, lineSp = 26f), ui.lp(MATCH, WRAP, b = 20))
        card.addView(ui.button(pack.t("continue")) { dismissOverlay() }, ui.lp(MATCH, WRAP))
        layer.addView(card, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.CENTER).apply {
            leftMargin = dp(24); rightMargin = dp(24)
        })
        // Particles burst from behind the card in theme colours.
        val confetti = ConfettiView(this, intArrayOf(pal.accent, pal.glow, pal.accentSoft, pal.muted))
        layer.addView(confetti, 0, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(layer, FrameLayout.LayoutParams(MATCH, MATCH))
        overlay = layer
        updateBackHandling()

        card.scaleX = 0.94f
        card.scaleY = 0.94f
        layer.animate().alpha(1f).setDuration(200).setInterpolator(kitEase()).start()
        card.animate().scaleX(1f).scaleY(1f).setDuration(240).setInterpolator(kitEase()).start()
        card.post {
            confetti.originY = card.top + card.height / 2f
            confetti.burst(big)
        }
        layer.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
    }

    fun dismissOverlay() {
        val o = overlay ?: return
        overlay = null
        o.animate().alpha(0f).setDuration(180).withEndAction { root.removeView(o) }.start()
        updateBackHandling()
    }
}
