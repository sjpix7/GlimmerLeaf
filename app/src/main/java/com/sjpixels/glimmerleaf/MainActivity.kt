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
    private lateinit var nav: LinearLayout
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

        column.addView(buildTopBar(ui), LinearLayout.LayoutParams(MATCH, dp(54)))
        host = FrameLayout(this)
        column.addView(host, LinearLayout.LayoutParams(MATCH, 0, 1f))
        nav = buildNav(ui)
        column.addView(nav, LinearLayout.LayoutParams(MATCH, WRAP))

        setContentView(root)
        applySystemBars()
    }

    private fun buildTopBar(ui: Ui): FrameLayout {
        val bar = FrameLayout(this)
        bar.setPadding(dp(6), 0, dp(10), 0)
        leftSlot = FrameLayout(this)
        bar.addView(leftSlot, FrameLayout.LayoutParams(dp(44), dp(44), Gravity.START or Gravity.CENTER_VERTICAL))

        val credit = TextView(this).apply {
            val prefix = "Created with Love by "
            val brand = "sj.pixels"
            val s = SpannableString(prefix + brand)
            s.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) = openUrl(CREDIT_URL)
                override fun updateDrawState(ds: TextPaint) {
                    ds.color = pal.accent
                    ds.isUnderlineText = true
                    ds.isFakeBoldText = true
                }
            }, prefix.length, prefix.length + brand.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text = s
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
            textSize = 11.5f
            setTextColor(pal.muted)
            typeface = Fonts.sans
            gravity = Gravity.CENTER
            maxLines = 1
        }
        bar.addView(credit, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))

        val toggle = DayNightToggle(this, pal, prefs.dark) { dark ->
            prefs.dark = dark
            applyTheme()
        }
        toggle.contentDescription = pack.t("night_mode")
        bar.addView(toggle, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.END or Gravity.CENTER_VERTICAL))
        return bar
    }

    private fun updateLeftSlot() {
        val ui = ui()
        leftSlot.removeAllViews()
        if (stack.isNotEmpty()) {
            val b = ui.iconButton(Icon.BACK, pack.t("close"), pal.ink, 44, 22) { handleBack() }
            leftSlot.addView(b, FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER))
        } else {
            val spark = IconView(this, Icon.SPARK, pal.accent, 18)
            leftSlot.addView(spark, FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER))
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

    private fun buildNav(ui: Ui): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(6), dp(6), dp(8))
            background = android.graphics.drawable.LayerDrawable(
                arrayOf(
                    android.graphics.drawable.ColorDrawable(withAlpha(pal.surface, 245)),
                    android.graphics.drawable.ColorDrawable(pal.line),
                )
            ).apply { setLayerHeight(1, dp(1)) }
        }
        navItems.forEachIndexed { i, item ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, dp(2), 0, dp(2))
            }
            val pill = FrameLayout(this)
            val icon = IconView(this, item.icon, pal.muted, 22)
            pill.addView(icon, FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
            cell.addView(pill, LinearLayout.LayoutParams(dp(58), dp(30)))
            val label = ui.text(pack.t(item.key), 11f, pal.muted, Fonts.sansMedium, 1f, Gravity.CENTER).apply {
                maxLines = 1
                setPadding(0, dp(3), 0, 0)
            }
            cell.addView(label, LinearLayout.LayoutParams(WRAP, WRAP))
            cell.background = ui.ripple(null, ui.rounded(Color.WHITE))
            cell.isClickable = true
            cell.contentDescription = pack.t(item.key)
            cell.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                selectTab(i)
            }
            cell.tag = Triple(pill, icon, label)
            bar.addView(cell, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        return bar
    }

    private fun Ui.rounded(color: Int) = rounded(color, dpf(18))

    private fun updateNav() {
        for (i in 0 until nav.childCount) {
            @Suppress("UNCHECKED_CAST")
            val parts = nav.getChildAt(i).tag as Triple<FrameLayout, IconView, TextView>
            val (pill, icon, label) = parts
            val sel = i == tab
            pill.background = if (sel) rounded(pal.accentSoft, dpf(15)) else null
            icon.color = if (sel) pal.accent else pal.muted
            label.setTextColor(if (sel) pal.ink else pal.muted)
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

    fun openRitual(id: String) = push(RitualScreen(this, id))

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
        d.window?.setBackgroundDrawable(rounded(pal.surface, dpf(24)))
        for (b in intArrayOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL)) {
            d.getButton(b)?.setTextColor(pal.accent)
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
            setBackgroundColor(withAlpha(if (pal.dark) Color.BLACK else pal.ink, 120))
            isClickable = true
            alpha = 0f
        }
        val confetti = ConfettiView(this, intArrayOf(pal.accent, pal.glow, pal.accentSoft, blend(pal.accent, Color.WHITE, 0.4f), 0xFFE9857A.toInt(), 0xFF8FCB9B.toInt()))
        layer.addView(confetti, FrameLayout.LayoutParams(MATCH, MATCH))

        val card = ui.col(26, 26).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            background = GlowDrawable(pal, dpf(28), resources.displayMetrics.density, 11)
            elevation = dpf(12)
            clipToOutline = true
        }
        card.addView(IconView(this, if (result.cycleCompleted) Icon.HEART else Icon.SPARK, pal.accent, 44), ui.lp(dp(44), dp(44), b = 10))
        card.addView(ui.text(title, 32f, pal.ink, Fonts.serifBold, 1.1f, Gravity.CENTER), ui.lp(WRAP, WRAP, b = 8))
        card.addView(ui.text(body, 16f, pal.ink, Fonts.sans, 1.4f, Gravity.CENTER), ui.lp(MATCH, WRAP, b = 6))
        card.addView(ui.text(d.affirmation, 14.5f, pal.muted, Fonts.serifItalic, 1.35f, Gravity.CENTER), ui.lp(MATCH, WRAP, t = 4, b = 20))
        card.addView(ui.button(pack.t("continue")) { dismissOverlay() }, ui.lp(MATCH, WRAP))
        layer.addView(card, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.CENTER).apply {
            leftMargin = dp(28); rightMargin = dp(28)
        })
        root.addView(layer, FrameLayout.LayoutParams(MATCH, MATCH))
        overlay = layer
        updateBackHandling()

        card.scaleX = 0.85f
        card.scaleY = 0.85f
        layer.animate().alpha(1f).setDuration(220).start()
        card.animate().scaleX(1f).scaleY(1f).setDuration(420).setInterpolator(android.view.animation.OvershootInterpolator(1.6f)).start()
        confetti.burst(if (result.cycleCompleted) 260 else 150)
        layer.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
    }

    fun dismissOverlay() {
        val o = overlay ?: return
        overlay = null
        o.animate().alpha(0f).setDuration(180).withEndAction { root.removeView(o) }.start()
        updateBackHandling()
    }
}
