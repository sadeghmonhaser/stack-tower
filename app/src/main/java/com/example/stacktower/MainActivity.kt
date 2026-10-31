package com.example.stacktower

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.net.Uri
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.EditText
import android.widget.ScrollView
import android.text.InputFilter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : Activity() {
    private var game: GameView? = null
    private lateinit var ui: OverlayUi
    private lateinit var pauseBtn: Button
    private lateinit var overlayContainer: FrameLayout
    private lateinit var homeCard: LinearLayout
    private lateinit var tutorialCard: LinearLayout
    private lateinit var themesCard: LinearLayout
    private lateinit var homeGreeting: TextView
    private lateinit var homeStatValue: TextView
    private var currentCard: LinearLayout? = null
    private var tutorialStartsGame = false
    private lateinit var menuCard: LinearLayout
    private lateinit var settingsCard: LinearLayout
    private lateinit var gameOverCard: LinearLayout
    private lateinit var aboutCard: LinearLayout
    private lateinit var profileCard: LinearLayout
    private lateinit var profileName: EditText
    private lateinit var profileStats: TextView

    private lateinit var tvGameOverScoreHero: TextView
    private lateinit var tvGameOverBest: TextView
    private lateinit var tvNewRecordBadge: TextView
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("stack", MODE_PRIVATE)
        ui = OverlayUi(this)

        val root = FrameLayout(this)
        val g = GameView(this)
        game = g
        root.addView(g, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        pauseBtn = ui.createChromeButton(
            getString(R.string.pause_menu),
            getString(R.string.pause_menu_content_description)
        ) { showPauseMenu() }
        val pauseParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.END
        )
        root.addView(pauseBtn, pauseParams)
        pauseBtn.bringToFront()
        applyPauseInsets(pauseParams)
        bindPauseExclusionToGame()

        setupOverlays(root)
        setContentView(root)

        g.onGameOverListener = { finalScore, bestScore, previousBest, isNewRecord ->
            runOnUiThread { showGameOverMenu(finalScore, bestScore, previousBest, isNewRecord) }
        }
        showHome()
    }

    private fun statusBarHeightPx(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else ui.dp(24)
    }

    private fun applyPauseInsets(pauseParams: FrameLayout.LayoutParams) {
        val topGap = ui.dimenPx(R.dimen.pause_margin_top)
        val endGap = ui.dimenPx(R.dimen.pause_margin_end)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.decorView.setOnApplyWindowInsetsListener { _, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                pauseParams.setMargins(0, bars.top + topGap, bars.right + endGap, 0)
                pauseBtn.layoutParams = pauseParams
                bindPauseExclusionToGame()
                insets
            }
        } else {
            pauseParams.setMargins(0, statusBarHeightPx() + topGap, endGap, 0)
            pauseBtn.layoutParams = pauseParams
        }
    }

    private fun bindPauseExclusionToGame() {
        pauseBtn.post {
            game?.setPauseExclusionRect(pauseBtn.left, pauseBtn.top, pauseBtn.right, pauseBtn.bottom)
        }
    }

    private fun setupOverlays(root: FrameLayout) {
        overlayContainer = FrameLayout(this).apply {
            setBackgroundColor(ui.color(R.color.overlay_scrim))
            visibility = View.GONE
            setOnClickListener { }
        }

        val cardLayoutParams = FrameLayout.LayoutParams(
            ui.cardWidthPx(),
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )

        val (statRow, statValue) = ui.createStatChipRow(getString(R.string.home_stat_record))
        homeStatValue = statValue

        homeCard = ui.createCardLayout().apply {
            addView(ui.createHeroTitle(getString(R.string.app_name)))
            homeGreeting = ui.createSubtitleText()
            addView(homeGreeting)
            addView(statRow)
            addView(ui.createCardButtonPrimary(getString(R.string.start_game)) { startOrTutorial() }.apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = ui.dimenPx(R.dimen.space_sm)
            })
            addView(ui.createSubtleDivider())
            addView(ui.createSectionLabel(getString(R.string.home_more_options)))
            addView(ui.createCardButtonSecondaryCompact(getString(R.string.profile)) { showProfile() })
            addView(ui.createCardButtonSecondaryCompact(getString(R.string.settings)) { openSettingsFromHub() })
            addView(ui.createCardButtonSecondaryCompact(getString(R.string.how_to_play)) {
                tutorialStartsGame = false
                rememberSubpageReturn()
                showCard(tutorialCard)
            })
            addView(ui.createCardButtonSecondaryCompact(getString(R.string.rate_app)) { launchRating() })
            addView(ui.createCardButtonSecondaryCompact(getString(R.string.about)) {
                rememberSubpageReturn()
                showCard(aboutCard)
            })
        }
        addScrollableCard(homeCard, cardLayoutParams)

        tutorialCard = ui.createCardLayout().apply {
            addView(ui.createHeaderTextView(getString(R.string.how_to_play)))
            addView(ui.createBodyText().apply {
                text = getString(R.string.tutorial_visual)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f)
                setTextColor(ui.color(R.color.tutorial_accent))
            })
            addView(ui.createBodyText().apply { text = getString(R.string.tutorial_text) })
            addView(ui.createCardButtonPrimary(getString(R.string.understood)) {
                prefs.edit().putBoolean("tutorial_seen", true).apply()
                if (tutorialStartsGame) startGame() else showHome()
            })
        }
        addScrollableCard(tutorialCard, cardLayoutParams)

        themesCard = ui.createCardLayout()
        addScrollableCard(themesCard, cardLayoutParams)

        menuCard = ui.createCardLayout().apply {
            addView(ui.createHeaderTextView(getString(R.string.menu_title)))
            addView(ui.createCardButtonPrimary(getString(R.string.resume)) { resumeGame() })
            addView(ui.createCardButtonSecondary(getString(R.string.new_game)) { startGame() })
            addView(ui.createCardButtonSecondary(getString(R.string.profile)) { showProfile() })
            addView(ui.createCardButtonSecondary(getString(R.string.settings)) { openSettingsFromPause() })
            addView(ui.createCardButtonSecondary(getString(R.string.rate_app)) { launchRating() })
            addView(ui.createCardButtonSecondary(getString(R.string.about)) {
                rememberSubpageReturn()
                showCard(aboutCard)
            })
        }
        addScrollableCard(menuCard, cardLayoutParams)

        settingsCard = ui.createCardLayout().apply {
            addView(ui.createHeaderTextView(getString(R.string.settings)))

            val cbSound = CheckBox(this@MainActivity).apply {
                text = getString(R.string.sound_fx)
                setTextColor(ui.color(R.color.text_primary))
                setTextSize(
                    android.util.TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(R.dimen.text_body)
                )
                isChecked = prefs.getBoolean("sound", true)
                contentDescription = getString(R.string.sound_fx)
                setOnCheckedChangeListener { _, checked ->
                    prefs.edit().putBoolean("sound", checked).apply()
                    game?.soundManager?.enabled = checked
                }
            }
            val cbHaptic = CheckBox(this@MainActivity).apply {
                text = getString(R.string.haptic_feedback)
                setTextColor(ui.color(R.color.text_primary))
                setTextSize(
                    android.util.TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(R.dimen.text_body)
                )
                isChecked = prefs.getBoolean("haptic", true)
                contentDescription = getString(R.string.haptic_feedback)
                setOnCheckedChangeListener { _, checked ->
                    prefs.edit().putBoolean("haptic", checked).apply()
                }
            }

            val cbLayout = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(ui.dimenPx(R.dimen.space_md), ui.dimenPx(R.dimen.space_md), ui.dimenPx(R.dimen.space_md), ui.dimenPx(R.dimen.space_lg))
                addView(cbSound)
                addView(cbHaptic)
            }
            addView(cbLayout)
            addView(ui.createCardButtonSecondary(getString(R.string.themes)) { showThemes() })
            addView(ui.createCardButtonSecondary(getString(R.string.close)) { returnFromSubpage() })
        }
        addScrollableCard(settingsCard, cardLayoutParams)

        gameOverCard = ui.createCardLayout().apply {
            addView(ui.createHeaderTextView(getString(R.string.game_over)))

            tvNewRecordBadge = TextView(this@MainActivity).apply {
                text = getString(R.string.new_record)
                setTextColor(ui.color(R.color.success_record))
                setTextSize(
                    android.util.TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(R.dimen.text_display_sm)
                )
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                visibility = View.GONE
                setPadding(0, ui.dimenPx(R.dimen.space_xs), 0, ui.dimenPx(R.dimen.space_sm))
            }
            addView(tvNewRecordBadge)

            addView(ui.createCaptionLabel(getString(R.string.game_over_your_score)))
            tvGameOverScoreHero = ui.createScoreHeroTextView()
            addView(tvGameOverScoreHero)

            val statsPanel = ui.createInsetPanel()
            tvGameOverBest = TextView(this@MainActivity).apply {
                setTextColor(ui.color(R.color.text_secondary))
                setTextSize(
                    android.util.TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(R.dimen.text_caption)
                )
                gravity = Gravity.CENTER
            }
            statsPanel.addView(tvGameOverBest)
            addView(statsPanel)

            addView(ui.createCardButtonPrimary(getString(R.string.retry)) { startGame() })
            addView(ui.createCardButtonSecondary(getString(R.string.share_score)) { shareScore() })
            addView(ui.createCardButtonSecondary(getString(R.string.go_home)) { showHome() })
        }
        addScrollableCard(gameOverCard, cardLayoutParams)

        aboutCard = ui.createCardLayout().apply {
            addView(ui.createHeaderTextView(getString(R.string.about)))

            val tvAbout = TextView(this@MainActivity).apply {
                text = getString(R.string.about_text)
                setTextColor(ui.color(R.color.text_secondary))
                setTextSize(
                    android.util.TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(R.dimen.text_caption)
                )
                setPadding(ui.dimenPx(R.dimen.space_lg), ui.dimenPx(R.dimen.space_md), ui.dimenPx(R.dimen.space_lg), ui.dimenPx(R.dimen.space_lg))
                gravity = Gravity.CENTER
            }
            addView(tvAbout)
            addView(ui.createCardButtonSecondary(getString(R.string.close)) { returnFromSubpage() })
        }
        addScrollableCard(aboutCard, cardLayoutParams)

        profileCard = ui.createCardLayout().apply {
            addView(ui.createHeaderTextView(getString(R.string.profile)))
            addView(TextView(this@MainActivity).apply {
                text = getString(R.string.profile_description)
                setTextColor(ui.color(R.color.text_secondary))
                setTextSize(
                    android.util.TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(R.dimen.text_caption)
                )
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, ui.dimenPx(R.dimen.space_md))
            })
            profileName = EditText(this@MainActivity).apply {
                hint = getString(R.string.profile_name_hint)
                setSingleLine(true)
                filters = arrayOf(InputFilter.LengthFilter(24))
                setTextColor(ui.color(R.color.text_primary))
                setHintTextColor(ui.color(R.color.text_hint))
                setTextSize(
                    android.util.TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(R.dimen.text_body)
                )
                textDirection = View.TEXT_DIRECTION_FIRST_STRONG
                minHeight = ui.dimenPx(R.dimen.button_min_height)
                background = ui.styledInputBackground()
                setPadding(ui.dimenPx(R.dimen.space_md), ui.dimenPx(R.dimen.space_sm), ui.dimenPx(R.dimen.space_md), ui.dimenPx(R.dimen.space_sm))
                contentDescription = getString(R.string.profile_name_hint)
            }
            addView(profileName)
            profileStats = TextView(this@MainActivity).apply {
                setTextColor(ui.color(R.color.text_primary))
                setTextSize(
                    android.util.TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(R.dimen.text_body)
                )
                gravity = Gravity.CENTER
                setPadding(0, ui.dimenPx(R.dimen.space_md), 0, ui.dimenPx(R.dimen.space_sm))
            }
            addView(profileStats)
            addView(ui.createCardButtonPrimary(getString(R.string.save_profile)) { saveProfile() })
            addView(ui.createCardButtonSecondary(getString(R.string.close)) { returnFromSubpage() })
        }
        addScrollableCard(profileCard, cardLayoutParams)

        root.addView(overlayContainer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
    }

    private fun addScrollableCard(card: LinearLayout, params: FrameLayout.LayoutParams) {
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            isVerticalScrollBarEnabled = true
            addView(card)
        }
        val bounded = FrameLayout.LayoutParams(params.width, ui.overlayMaxHeightPx(), Gravity.CENTER)
        overlayContainer.addView(scroll, bounded)
        scroll.visibility = View.GONE
    }

    private fun rememberSubpageReturn() {
        subpageReturnHome = (currentCard == homeCard)
    }

    private fun openSettingsFromHub() {
        rememberSubpageReturn()
        showCard(settingsCard)
    }

    private fun openSettingsFromPause() {
        subpageReturnHome = false
        showCard(settingsCard)
    }

    private fun resumeGame() {
        hideOverlays()
        game?.syncSkyThemeFromPrefs()
        game?.resumeLoop()
    }

    private fun showProfile() {
        rememberSubpageReturn()
        profileName.setText(prefs.getString("profile_name", ""))
        val number = NumberFormat.getIntegerInstance(Locale("fa"))
        profileStats.text = getString(R.string.profile_stats,
            number.format(prefs.getInt("best", 0)), number.format(prefs.getInt("games_played", 0)),
            number.format(prefs.getInt("last_score", 0)))
        val history = prefs.getString("recent_scores", "").orEmpty().split(",").mapNotNull { it.toIntOrNull() }
        profileStats.append("\n\n" + getString(R.string.recent_scores, if (history.isEmpty()) getString(R.string.no_scores) else history.joinToString("  •  ") { number.format(it) }))
        showCard(profileCard)
    }

    private fun saveProfile() {
        val name = profileName.text.toString().trim()
        if (name.isEmpty()) {
            profileName.error = getString(R.string.profile_name_required)
            return
        }
        prefs.edit().putString("profile_name", name).apply()
        Toast.makeText(this, getString(R.string.profile_saved), Toast.LENGTH_SHORT).show()
        returnFromSubpage()
    }

    private var subpageReturnHome = true

    private fun showHome() {
        game?.pauseLoop()
        subpageReturnHome = true
        val name = prefs.getString("profile_name", "").orEmpty()
        homeGreeting.text = if (name.isBlank()) getString(R.string.welcome) else getString(R.string.welcome_player, name)
        homeStatValue.text = NumberFormat.getIntegerInstance(Locale("fa")).format(prefs.getInt("best", 0))
        showCard(homeCard)
    }

    private fun startOrTutorial() {
        if (prefs.getBoolean("tutorial_seen", false)) startGame() else {
            tutorialStartsGame = true
            showCard(tutorialCard)
        }
    }

    private fun startGame() {
        hideOverlays()
        game?.syncSkyThemeFromPrefs()
        game?.startNewGame()
    }

    private fun returnFromSubpage() {
        if (subpageReturnHome) showHome() else showCard(menuCard)
    }

    private fun showThemes() {
        themesCard.removeAllViews()
        themesCard.addView(ui.createHeaderTextView(getString(R.string.themes)))
        themesCard.addView(ui.createBodyText().apply { text = getString(R.string.themes_hint) })
        val best = prefs.getInt("best", 0)
        val names = arrayOf(getString(R.string.theme_default), getString(R.string.theme_sunset), getString(R.string.theme_ocean), getString(R.string.theme_aurora))
        val goals = intArrayOf(0, 10, 25, 50)
        names.forEachIndexed { index, name ->
            val unlocked = best >= goals[index]
            val selected = prefs.getInt("theme", 0) == index
            val label = when {
                selected -> getString(R.string.theme_selected, name)
                unlocked -> name
                else -> getString(R.string.theme_locked, name, NumberFormat.getIntegerInstance(Locale("fa")).format(goals[index]))
            }
            val buttonFactory = if (selected) ui::createCardButtonPrimary else ui::createCardButtonSecondary
            themesCard.addView(buttonFactory(label) {
                if (!unlocked) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(
                            R.string.theme_locked_toast,
                            name,
                            NumberFormat.getIntegerInstance(Locale("fa")).format(goals[index])
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    prefs.edit().putInt("theme", index).commit()
                    game?.applySkyTheme(index)
                    showThemes()
                }
            }.apply { alpha = if (unlocked) 1f else 0.55f })
        }
        themesCard.addView(ui.createCardButtonSecondary(getString(R.string.close)) { showCard(settingsCard) })
        showCard(themesCard)
    }

    private fun showCard(cardToShow: LinearLayout) {
        currentCard = cardToShow
        for (i in 0 until overlayContainer.childCount) {
            overlayContainer.getChildAt(i).visibility = View.GONE
        }
        (cardToShow.parent as View).visibility = View.VISIBLE
        overlayContainer.visibility = View.VISIBLE
        pauseBtn.visibility = View.GONE
    }

    private fun showPauseMenu() {
        game?.pauseLoop()
        subpageReturnHome = false
        showCard(menuCard)
    }

    private fun showGameOverMenu(score: Int, best: Int, previousBest: Int, isNewRecord: Boolean) {
        tvGameOverScoreHero.text = game?.persianNumber(score) ?: NumberFormat.getIntegerInstance(Locale("fa")).format(score)
        tvGameOverBest.text = getString(
            R.string.results_bests,
            game?.persianNumber(previousBest) ?: "$previousBest",
            game?.persianNumber(best) ?: "$best"
        )
        val unlocked = listOf(10 to R.string.theme_sunset, 25 to R.string.theme_ocean, 50 to R.string.theme_aurora).filter { previousBest < it.first && best >= it.first }
        tvNewRecordBadge.text = if (unlocked.isNotEmpty()) getString(R.string.theme_unlocked, unlocked.joinToString("، ") { getString(it.second) }) else getString(R.string.new_record)
        tvNewRecordBadge.visibility = if (isNewRecord) View.VISIBLE else View.GONE
        showCard(gameOverCard)
    }

    private fun hideOverlays() {
        overlayContainer.visibility = View.GONE
        currentCard = null
        pauseBtn.visibility = View.VISIBLE
        bindPauseExclusionToGame()
        game?.syncSkyThemeFromPrefs()
    }

    private fun launchRating() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("myket://comment?id=$packageName")).apply {
                setPackage("ir.mservices.market")
            }
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.myket_not_installed), Toast.LENGTH_LONG).show()
        }
    }

    private fun shareScore() {
        val scoreStr = game?.persianNumber(game?.getScore() ?: 0) ?: "0"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, scoreStr))
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_score)))
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when (currentCard) {
            null -> showPauseMenu()
            homeCard -> super.onBackPressed()
            menuCard -> resumeGame()
            gameOverCard -> showHome()
            themesCard -> showCard(settingsCard)
            tutorialCard -> if (tutorialStartsGame) showHome() else returnFromSubpage()
            else -> returnFromSubpage()
        }
    }

    override fun onPause() { super.onPause(); game?.pauseLoop() }
    override fun onResume() { super.onResume(); if (!::overlayContainer.isInitialized || overlayContainer.visibility != View.VISIBLE) game?.resumeLoop() }
    override fun onDestroy() { super.onDestroy(); game?.soundManager?.release() }
}


class GameView(context: Context) : View(context) {

    val soundManager = SoundManager()
    var onGameOverListener: ((score: Int, best: Int, previousBest: Int, isNewRecord: Boolean) -> Unit)? = null

    private data class Block(val x: Float, val w: Float, val color: Int)
    private data class Debris(var x: Float, var y: Float, var w: Float, var vy: Float, val color: Int)
    private data class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var radius: Float, val color: Int, var alpha: Float)
    private data class Shockwave(var x: Float, var y: Float, var radius: Float, var maxRadius: Float, val color: Int, var alpha: Float)
    private data class Star(var x: Float, var y: Float, val radius: Float, var alpha: Float, val speed: Float)

    private val blocks = mutableListOf<Block>()
    private val debris = mutableListOf<Debris>()
    private val particles = mutableListOf<Particle>()
    private val shockwaves = mutableListOf<Shockwave>()
    private val stars = mutableListOf<Star>()

    private var moving: Block? = null
    private var dir = 1
    private var x = 0f
    private var speed = 0f
    private var y = 0f
    private var blockH = 0f
    private var score = 0
    private var combo = 0
    private var best = 0
    private var over = false

    private var popText = ""
    private var popAlpha = 0f
    private var popY = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 5f
    }

    private val prefs = context.getSharedPreferences("stack", Context.MODE_PRIVATE)

    private var running = false
    private var lastNs = 0L
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(ts: Long) {
            val dt = if (lastNs == 0L) 1f else ((ts - lastNs) / 16_666_667f).coerceAtMost(3f)
            lastNs = ts
            update(dt)
            if (running) Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private var skyThemeIndex = 0

    init {
        best = prefs.getInt("best", 0)
        soundManager.enabled = prefs.getBoolean("sound", true)
        skyThemeIndex = prefs.getInt("theme", 0).coerceIn(0, 3)
    }

    /** Called when user picks a sky theme (tower block colors unchanged). */
    fun applySkyTheme(index: Int) {
        skyThemeIndex = index.coerceIn(0, 3)
        postInvalidateOnAnimation()
        invalidate()
    }

    fun syncSkyThemeFromPrefs() {
        applySkyTheme(prefs.getInt("theme", 0))
    }

    fun getScore() = score

    fun pauseLoop() {
        running = false; lastNs = 0L
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun resumeLoop() {
        if (!running) {
            running = true; lastNs = 0L
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        blockH = w * 0.09f
        y = h * 0.82f
        initStars()
        resetGame()
    }

    private fun initStars() {
        stars.clear()
        for (_i in 0..45) {
            stars.add(
                Star(
                    Random.nextFloat() * width,
                    Random.nextFloat() * height,
                    Random.nextFloat() * 2.5f + 1f,
                    Random.nextFloat() * 0.7f + 0.3f,
                    Random.nextFloat() * 0.4f + 0.1f
                )
            )
        }
    }

    fun startNewGame() { resetGame(); resumeLoop(); invalidate() }

    private fun resetGame() {
        blocks.clear(); debris.clear(); particles.clear(); shockwaves.clear()
        val baseW = width * 0.55f
        blocks.add(Block((width - baseW) / 2f, baseW, colorFor(0)))
        score = 0; combo = 0; over = false
        speed = width * 0.006f
        spawn()
    }

    private fun skyGradientColors(): IntArray {
        val drift = (blocks.size * 2 % 24).toFloat()
        return when (skyThemeIndex) {
            1 -> intArrayOf(
                blendHueToward(resColor(R.color.sky_theme1_top), drift, 8f),
                resColor(R.color.sky_theme1_bottom)
            )
            2 -> intArrayOf(
                blendHueToward(resColor(R.color.sky_theme2_top), drift, 6f),
                resColor(R.color.sky_theme2_bottom)
            )
            3 -> intArrayOf(
                blendHueToward(resColor(R.color.sky_theme3_top), drift, 10f),
                resColor(R.color.sky_theme3_bottom)
            )
            else -> intArrayOf(
                blendHueToward(resColor(R.color.sky_theme0_top), drift, 6f),
                resColor(R.color.sky_bottom)
            )
        }
    }

    /** Slight sky shift as tower grows — does not affect blocks. */
    private fun blendHueToward(color: Int, drift: Float, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[0] = (hsv[0] + drift * amount) % 360f
        return Color.HSVToColor(hsv)
    }

    private fun resColor(resId: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(resId)
        } else {
            @Suppress("DEPRECATION")
            context.resources.getColor(resId)
        }

    private fun colorFor(i: Int): Int =
        Color.HSVToColor(floatArrayOf((i * 17f) % 360f, 0.65f, 0.95f))

    private fun spawn() {
        val top = blocks.last()
        moving = Block(0f, top.w, colorFor(blocks.size))
        dir = if (blocks.size % 2 == 0) 1 else -1
        x = if (dir > 0) 0f else width - top.w
    }

    private fun update(dt: Float) {
        if (width == 0) return

        // Update star movement
        for (s in stars) {
            s.y += s.speed * dt
            if (s.y > height) {
                s.y = 0f
                s.x = Random.nextFloat() * width
            }
        }

        if (!over) {
            val m = moving
            if (m != null) {
                x += speed * dir * dt
                val limit = width - m.w
                if (x <= 0f) { x = 0f; dir = 1 }
                if (x >= limit) { x = limit; dir = -1 }
            }
        }

        val debrisIter = debris.iterator()
        while (debrisIter.hasNext()) {
            val d = debrisIter.next()
            d.vy += 0.9f * dt
            d.y += d.vy * dt
            if (d.y > height) debrisIter.remove()
        }

        val partIter = particles.iterator()
        while (partIter.hasNext()) {
            val p = partIter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.alpha -= 0.025f * dt
            p.radius *= 0.98f
            if (p.alpha <= 0f || p.radius <= 0.2f) partIter.remove()
        }

        val shockIter = shockwaves.iterator()
        while (shockIter.hasNext()) {
            val sw = shockIter.next()
            sw.radius += 8f * dt
            sw.alpha -= 0.04f * dt
            if (sw.alpha <= 0f || sw.radius >= sw.maxRadius) shockIter.remove()
        }

        if (popAlpha > 0f) { popAlpha -= 0.02f * dt; popY -= 1.2f * dt }
        invalidate()
    }

    private val pauseExclusionRect = RectF()
    private var pauseExclusionPadPx = 0f

    fun setPauseExclusionRect(left: Int, top: Int, right: Int, bottom: Int) {
        pauseExclusionRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        pauseExclusionPadPx = 8f * resources.displayMetrics.density
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (pauseExclusionRect.width() > 0f) {
            val pad = pauseExclusionPadPx
            if (e.x >= pauseExclusionRect.left - pad && e.x <= pauseExclusionRect.right + pad &&
                e.y >= pauseExclusionRect.top - pad && e.y <= pauseExclusionRect.bottom + pad
            ) {
                return false
            }
        }
        if (e.action == MotionEvent.ACTION_DOWN) {
            if (over) resetGame() else {
                if (prefs.getBoolean("haptic", true)) performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                drop()
            }
        }
        return true
    }

    private fun drop() {
        val m = moving ?: return
        val top = blocks.last()
        val mx = x
        val left = max(mx, top.x)
        val right = min(mx + m.w, top.x + top.w)
        val overlap = right - left

        if (overlap <= 0f) {
            debris.add(Debris(mx, y - blocks.size * blockH, m.w, 0f, m.color))
            soundManager.playGameOver()
            endGame()
            return
        }

        val perfect = abs(mx - top.x) < width * 0.015f && abs(m.w - top.w) < 2.5f
        val newBlock: Block

        if (perfect) {
            combo++
            newBlock = Block(top.x, top.w, m.color)
            popText = if (combo >= 3) "عالی! ×${persianNumber(combo)}" else "عالی!"
            soundManager.playPerfect(combo)
            spawnParticles(top.x + top.w / 2f, y - blocks.size * blockH, m.color)
        } else {
            combo = 0
            newBlock = Block(left, overlap, m.color)
            soundManager.playDrop(blocks.size)
            val cutLeft = if (mx < top.x) mx else right
            val cutW = m.w - overlap
            if (cutW > 0f) debris.add(Debris(cutLeft, y - blocks.size * blockH, cutW, 0f, m.color))
        }

        blocks.add(newBlock)
        score++
        popAlpha = if (perfect) 1f else 0f
        popY = y - blocks.size * blockH - blockH
        if (!perfect) popText = ""
        if (score % 5 == 0) speed *= 1.08f else speed *= 1.02f
        spawn()
        invalidate()
    }

    private fun spawnParticles(cx: Float, cy: Float, color: Int) {
        shockwaves.add(Shockwave(cx, cy, 10f, width * 0.35f, Color.WHITE, 0.9f))
        for (_i in 0..22) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val spd = Random.nextFloat() * 12f + 4f
            particles.add(
                Particle(
                    cx, cy,
                    cos(angle) * spd,
                    sin(angle) * spd - 3f,
                    Random.nextFloat() * 6f + 3f,
                    color,
                    1f
                )
            )
        }
    }

    private fun endGame() {
        over = true
        moving = null
        val previousBest = best
        var isNewRecord = false
        if (score > best) {
            best = score
            isNewRecord = true
            prefs.edit().putInt("best", best).apply()
        }
        val recent = (listOf(score) + prefs.getString("recent_scores", "").orEmpty().split(",").mapNotNull { it.toIntOrNull() }).take(5).joinToString(",")
        prefs.edit().putInt("games_played", prefs.getInt("games_played", 0) + 1)
            .putInt("last_score", score).putString("recent_scores", recent).apply()
        onGameOverListener?.invoke(score, best, previousBest, isNewRecord)
    }

    fun persianNumber(value: Int): String = NumberFormat.getIntegerInstance(Locale("fa")).format(value)

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()

        // Background gradient — sky theme + tower height
        val sky = skyGradientColors()
        paint.shader = LinearGradient(0f, 0f, 0f, h, sky[0], sky[1], Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Render Starfield
        paint.color = Color.WHITE
        for (s in stars) {
            paint.alpha = (s.alpha * 255).toInt().coerceIn(0, 255)
            c.drawCircle(s.x, s.y, s.radius, paint)
        }
        paint.alpha = 255

        val maxVisible = (y / blockH).toInt()
        val shift = if (blocks.size > maxVisible - 3) (blocks.size - (maxVisible - 3)) * blockH else 0f

        // Render Shockwaves
        glowPaint.style = Paint.Style.STROKE
        for (sw in shockwaves) {
            glowPaint.color = sw.color
            glowPaint.alpha = (sw.alpha * 255).toInt().coerceIn(0, 255)
            glowPaint.strokeWidth = 6f
            c.drawCircle(sw.x, sw.y + shift, sw.radius, glowPaint)
        }

        // Render Stacked Blocks with 3D Gloss
        for ((i, b) in blocks.withIndex()) {
            val by = y - i * blockH + shift
            paint.color = b.color
            c.drawRoundRect(b.x, by - blockH + 4f, b.x + b.w, by, 12f, 12f, paint)

            // Top Gloss Highlight
            paint.color = Color.argb(90, 255, 255, 255)
            c.drawRoundRect(b.x + 2f, by - blockH + 4f, b.x + b.w - 2f, by - blockH + 10f, 6f, 6f, paint)

            // Bottom Shadow Bevel
            paint.color = Color.argb(60, 0, 0, 0)
            c.drawRect(b.x + 2f, by - 5f, b.x + b.w - 2f, by, paint)
        }

        // Render Falling Debris
        for (d in debris) {
            paint.color = d.color
            c.drawRect(d.x, d.y + shift, d.x + d.w, d.y + blockH + shift, paint)
        }

        // Render Moving Active Block with Pulse Glow
        moving?.let { m ->
            val my = y - blocks.size * blockH + shift
            paint.color = m.color
            c.drawRoundRect(x, my - blockH + 4f, x + m.w, my, 12f, 12f, paint)

            val glowAlpha = (120 + 80 * sin(System.currentTimeMillis() / 120.0)).toInt().coerceIn(0, 255)
            glowPaint.color = Color.argb(glowAlpha, 255, 255, 255)
            glowPaint.strokeWidth = 4f
            c.drawRoundRect(x - 2f, my - blockH + 2f, x + m.w + 2f, my + 2f, 14f, 14f, glowPaint)
        }

        // Render Explosive Particles
        for (p in particles) {
            paint.color = p.color
            paint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
            c.drawCircle(p.x, p.y + shift, p.radius, paint)
        }
        paint.alpha = 255

        // Base Line
        paint.color = Color.argb(70, 255, 255, 255)
        c.drawRect(0f, y + shift, w, y + shift + 3f, paint)

        // In-Game Score Display (compact HUD)
        textPaint.textSize = w * 0.11f
        c.drawText(persianNumber(score), w / 2f, h * 0.11f, textPaint)
        textPaint.textSize = w * 0.038f
        c.drawText(context.getString(R.string.best_label, persianNumber(best)), w / 2f, h * 0.11f + w * 0.055f, textPaint)

        // Perfect Combo Popup
        if (popAlpha > 0f && popText.isNotEmpty()) {
            textPaint.textSize = w * 0.08f
            textPaint.color = resColor(R.color.success_record)
            textPaint.alpha = (popAlpha * 255).toInt().coerceIn(0, 255)
            c.drawText(popText, w / 2f, popY + shift, textPaint)
            textPaint.color = Color.WHITE
            textPaint.alpha = 255
        }

        // Tap to start guide
        if (!over && blocks.size == 1 && score == 0) {
            textPaint.textSize = w * 0.055f
            c.drawText(context.getString(R.string.drop_hint), w / 2f, h * 0.62f, textPaint)
        }
    }
}
