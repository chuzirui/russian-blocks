package com.russianblocks.game

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
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

        /*
         * These are Google's official test ad unit IDs.
         * Replace with your real IDs before publishing to the Play Store.
         * https://developers.google.com/admob/android/test-ads
         */
        private const val BANNER_AD_UNIT    = "ca-app-pub-3940256099942544/6300978111"
        private const val INTERSTITIAL_UNIT = "ca-app-pub-3940256099942544/1033173712"
    }

    private lateinit var gameView: GameView
    private lateinit var startOverlay: LinearLayout
    private lateinit var controlsBar: LinearLayout
    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var adBanner: AdView

    private var interstitialAd: InterstitialAd? = null
    private var gamesPlayed = 0

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
        btnPause = findViewById(R.id.btnPause)
        adBanner = findViewById(R.id.adBanner)

        initAds()

        btnStart.setOnClickListener {
            startOverlay.visibility = View.GONE
            controlsBar.visibility = View.VISIBLE
            adBanner.visibility = View.VISIBLE
            gameView.startGame()
        }

        btnPause.setOnClickListener {
            if (gameView.isPaused) {
                gameView.resumeGame()
                btnPause.text = getString(R.string.pause)
            } else {
                gameView.pauseGame()
                btnPause.text = getString(R.string.resume)
            }
        }

        setupRepeatingButton(R.id.btnLeft)  { gameView.moveLeft() }
        setupRepeatingButton(R.id.btnRight) { gameView.moveRight() }
        setupRepeatingButton(R.id.btnDown)  { gameView.softDrop() }

        findViewById<Button>(R.id.btnRotate).setOnClickListener { gameView.rotateCW() }
        findViewById<Button>(R.id.btnDrop).setOnClickListener   { gameView.hardDrop() }

        gameView.gameOverCallback = { finalScore ->
            runOnUiThread {
                gamesPlayed++
                showGameOverWithAd(finalScore)
            }
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
        AlertDialog.Builder(this, R.style.GameOverDialog)
            .setTitle(getString(R.string.game_over))
            .setMessage(getString(R.string.final_score, finalScore))
            .setPositiveButton(getString(R.string.play_again)) { _, _ ->
                gameView.startGame()
                btnPause.text = getString(R.string.pause)
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
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
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
            btnPause.text = getString(R.string.resume)
        }
    }

    override fun onResume() {
        super.onResume()
        adBanner.resume()
    }

    override fun onDestroy() {
        adBanner.destroy()
        super.onDestroy()
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
