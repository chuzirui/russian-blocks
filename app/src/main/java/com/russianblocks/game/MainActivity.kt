package com.russianblocks.game

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RussianBlocks"
        private const val PREFS_NAME = "russian_blocks_prefs"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_BOMBS = "bombs"

        private const val BANNER_AD_UNIT    = "ca-app-pub-3940256099942544/6300978111"
        private const val INTERSTITIAL_UNIT = "ca-app-pub-3940256099942544/1033173712"
    }

    private lateinit var gameView: GameView
    private lateinit var startOverlay: FrameLayout
    private lateinit var controlsBar: LinearLayout
    private lateinit var btnStart: Button
    private lateinit var btnBomb: Button
    private lateinit var adBanner: AdView

    private var interstitialAd: InterstitialAd? = null
    private var gamesPlayed = 0
    private lateinit var billingManager: BillingManager
    private lateinit var soundManager: GameSoundManager

    private val repeatHandler = Handler(Looper.getMainLooper())
    private val repeatDelay = 120L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemUI()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        gameView = findViewById(R.id.gameView)
        startOverlay = findViewById(R.id.startOverlay)
        controlsBar = findViewById(R.id.controlsBar)
        btnStart = findViewById(R.id.btnStart)
        btnBomb = findViewById(R.id.btnBomb)
        adBanner = findViewById(R.id.adBanner)

        gameView.highScore = loadHighScore()

        soundManager = GameSoundManager(this)
        gameView.soundManager = soundManager

        billingManager = BillingManager(this) { amount ->
            addBombs(amount)
            Toast.makeText(this, "+$amount Bombs!", Toast.LENGTH_SHORT).show()
        }
        billingManager.connect()

        initAds()

        findViewById<Button>(R.id.btnShop).setOnClickListener { v ->
            tapHaptic(v)
            showShopDialog()
        }

        btnStart.setOnClickListener { v ->
            tapHaptic(v)
            startOverlay.visibility = View.GONE
            controlsBar.visibility = View.VISIBLE
            adBanner.visibility = View.VISIBLE
            gameView.highScore = loadHighScore()
            gameView.startGame()
            loadBombsIntoEngine()
        }

        btnBomb.setOnClickListener { v ->
            if (gameView.bombs > 0) {
                tapHaptic(v)
                gameView.useBomb()
            }
        }

        setupRepeatingButton(R.id.btnLeft)  { gameView.moveLeft() }
        setupRepeatingButton(R.id.btnRight) { gameView.moveRight() }
        setupRepeatingButton(R.id.btnDown)  { gameView.softDrop() }

        findViewById<Button>(R.id.btnRotate).setOnClickListener { v ->
            tapHaptic(v)
            gameView.rotateCW()
        }
        findViewById<Button>(R.id.btnDrop).setOnClickListener { v ->
            tapHaptic(v)
            gameView.hardDrop()
        }

        gameView.bombCountCallback = { bombs ->
            runOnUiThread {
                saveBombs(bombs)
                updateBombButton(bombs)
            }
        }

        gameView.highScoreBrokenCallback = { newScore ->
            runOnUiThread {
                saveBombs(gameView.bombs)
                Toast.makeText(this, "NEW HIGH SCORE! Bomb earned!", Toast.LENGTH_SHORT).show()
            }
        }

        gameView.gameOverCallback = { finalScore ->
            runOnUiThread {
                gamesPlayed++
                saveHighScore(finalScore)
                showGameOverWithAd(finalScore)
            }
        }
    }

    /* ── High Score ─────────────────────────────────────── */

    private fun loadHighScore(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_HIGH_SCORE, 0)
    }

    private fun saveHighScore(score: Int) {
        val current = loadHighScore()
        if (score > current) {
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_HIGH_SCORE, score).apply()
            gameView.highScore = score
        }
    }

    /* ── Bomb persistence ────────────────────────────────── */

    private fun loadBombs(): Int {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BOMBS, 0)
    }

    private fun saveBombs(count: Int) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_BOMBS, count).apply()
    }

    private fun loadBombsIntoEngine() {
        val saved = loadBombs()
        gameView.bombs = saved
        updateBombButton(saved)
    }

    private fun addBombs(amount: Int) {
        gameView.bombs += amount
        saveBombs(gameView.bombs)
        updateBombButton(gameView.bombs)
    }

    /* ── Shop ─────────────────────────────────────────────── */

    private fun showShopDialog(onClose: (() -> Unit)? = null) {
        val products = BillingManager.PRODUCTS
        val items = products.map { "${it.title}  —  ${it.price}" }.toTypedArray()

        AlertDialog.Builder(this, R.style.GameOverDialog)
            .setTitle(R.string.bomb_shop)
            .setItems(items) { _, which ->
                billingManager.launchPurchase(products[which].id)
                onClose?.invoke()
            }
            .setNegativeButton("Close") { _, _ ->
                onClose?.invoke()
            }
            .setCancelable(false)
            .show()
    }

    /* ── Bomb Button ────────────────────────────────────── */

    private fun updateBombButton(bombs: Int) {
        btnBomb.isEnabled = bombs > 0
        if (bombs > 0) {
            btnBomb.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.ds_bomb_active)
            )
            btnBomb.text = getString(R.string.bomb_count_fmt, bombs)
        } else {
            btnBomb.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.ds_control_idle)
            )
            btnBomb.text = getString(R.string.bomb_label)
        }
    }

    /* ── Ads ────────────────────────────────────────────── */

    private fun initAds() {
        MobileAds.initialize(this) { status ->
            Log.d(TAG, "AdMob initialized: $status")
        }
        adBanner.loadAd(AdRequest.Builder().build())
        loadInterstitial()
    }

    private fun loadInterstitial() {
        InterstitialAd.load(
            this,
            INTERSTITIAL_UNIT,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    private fun showGameOverWithAd(finalScore: Int) {
        val showAd = gamesPlayed % 3 == 0 && interstitialAd != null
        if (showAd) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial()
                    showGameOverDialog(finalScore)
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadInterstitial()
                    showGameOverDialog(finalScore)
                }
            }
            interstitialAd?.show(this)
        } else {
            showGameOverDialog(finalScore)
        }
    }

    private fun showGameOverDialog(finalScore: Int) {
        val highScore = loadHighScore()
        val isNewBest = finalScore >= highScore
        val msg = if (isNewBest)
            getString(R.string.final_score_best, finalScore)
        else
            getString(R.string.final_score_with_best, finalScore, highScore)

        AlertDialog.Builder(this, R.style.GameOverDialog)
            .setTitle(getString(R.string.game_over))
            .setMessage(msg)
            .setPositiveButton(getString(R.string.play_again)) { _, _ ->
                gameView.highScore = loadHighScore()
                gameView.startGame()
                loadBombsIntoEngine()
            }
            .setNeutralButton(R.string.buy_bombs) { _, _ ->
                showShopDialog { showGameOverDialog(finalScore) }
            }
            .setNegativeButton(getString(R.string.quit)) { _, _ ->
                controlsBar.visibility = View.GONE
                startOverlay.visibility = View.VISIBLE
            }
            .setCancelable(false)
            .show()
    }

    /* ── Controls ───────────────────────────────────────── */

    @SuppressLint("ClickableViewAccessibility")
    private fun setupRepeatingButton(id: Int, action: () -> Unit) {
        val button = findViewById<Button>(id)
        var repeating = false
        val repeatRunnable = object : Runnable {
            override fun run() {
                if (repeating) {
                    action()
                    repeatHandler.postDelayed(this, repeatDelay)
                }
            }
        }
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    tapHaptic(v)
                    repeating = true
                    action()
                    repeatHandler.postDelayed(repeatRunnable, repeatDelay * 2)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    repeating = false
                    repeatHandler.removeCallbacks(repeatRunnable)
                    true
                }
                else -> false
            }
        }
    }

    /* ── Lifecycle ──────────────────────────────────────── */

    override fun onPause() {
        adBanner.pause()
        super.onPause()
        if (!gameView.isGameOver) {
            gameView.pauseGame()
        }
    }

    override fun onResume() {
        super.onResume()
        adBanner.resume()
        if (!gameView.isGameOver) {
            gameView.resumeGame()
        }
    }

    override fun onDestroy() {
        gameView.soundManager = null
        soundManager.release()
        adBanner.destroy()
        billingManager.destroy()
        super.onDestroy()
    }

    private fun tapHaptic(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
