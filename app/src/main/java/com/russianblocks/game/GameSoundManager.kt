package com.russianblocks.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import kotlin.random.Random

/**
 * Short SFX via [SoundPool]. Samples are **Kenney.nl** CC0 packs (UI Audio + Impact
 * Sounds); full license text is under `assets/licenses_kenney_*.txt`.
 *
 * [playMove] and [playLineClear] pick **random variants** plus light pitch jitter so
 * repeats feel less mechanical. Gameplay ticks are rate-limited for hold-to-repeat.
 */
class GameSoundManager(context: Context) {

    private val ctx = context.applicationContext

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val idMove = IntArray(3)
    private var idRotate = 0
    private var idSoftDrop = 0
    private var idHardDrop = 0
    private var idLock = 0
    private val idLineClear = IntArray(2)
    private var idBomb = 0
    private var idChain = 0

    private var loadsOk = 0
    private val loadsExpected = 11

    private var lastMoveMs = 0L
    private var lastSoftMs = 0L

    init {
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) loadsOk++
        }
        idMove[0] = soundPool.load(ctx, R.raw.sound_move_1, 1)
        idMove[1] = soundPool.load(ctx, R.raw.sound_move_2, 1)
        idMove[2] = soundPool.load(ctx, R.raw.sound_move_3, 1)
        idRotate = soundPool.load(ctx, R.raw.sound_rotate, 1)
        idSoftDrop = soundPool.load(ctx, R.raw.sound_soft_drop, 1)
        idHardDrop = soundPool.load(ctx, R.raw.sound_hard_drop, 1)
        idLock = soundPool.load(ctx, R.raw.sound_lock, 1)
        idLineClear[0] = soundPool.load(ctx, R.raw.sound_line_clear_1, 1)
        idLineClear[1] = soundPool.load(ctx, R.raw.sound_line_clear_2, 1)
        idBomb = soundPool.load(ctx, R.raw.sound_bomb, 1)
        idChain = soundPool.load(ctx, R.raw.sound_chain, 1)
    }

    private fun ready(): Boolean = loadsOk >= loadsExpected

    private fun playIfReady(id: Int, left: Float, right: Float, rate: Float = 1f) {
        if (!ready() || id == 0) return
        soundPool.play(id, left, right, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    private fun rateJitter(): Float = 1f + (Random.nextFloat() - 0.5f) * JITTER

    fun playMove() {
        val now = SystemClock.uptimeMillis()
        if (now - lastMoveMs < MOVE_REPEAT_MS) return
        lastMoveMs = now
        val id = idMove.random()
        playIfReady(id, 0.55f, 0.55f, rateJitter())
    }

    fun playRotate() {
        playIfReady(idRotate, 0.62f, 0.62f, rateJitter())
    }

    fun playSoftDrop() {
        val now = SystemClock.uptimeMillis()
        if (now - lastSoftMs < SOFT_REPEAT_MS) return
        lastSoftMs = now
        playIfReady(idSoftDrop, 0.42f, 0.42f, rateJitter())
    }

    fun playHardDrop(rowsDropped: Int) {
        val base = when {
            rowsDropped >= 12 -> 1.05f
            rowsDropped >= 6 -> 1f
            else -> 0.94f
        }
        playIfReady(idHardDrop, 0.78f, 0.78f, base * rateJitter())
    }

    fun playLock() {
        playIfReady(idLock, 0.5f, 0.5f, rateJitter())
    }

    fun playLineClear(lines: Int) {
        if (!ready()) return
        val id = idLineClear.random()
        if (id == 0) return
        val base = when {
            lines >= 5 -> 1.22f
            lines >= 3 -> 1.1f
            else -> 1f
        }
        playIfReady(id, 0.88f, 0.88f, base * rateJitter())
    }

    fun playBombExplosion() {
        playIfReady(idBomb, 0.92f, 0.92f, rateJitter())
    }

    fun playChainGravity() {
        playIfReady(idChain, 0.55f, 0.55f, rateJitter())
    }

    fun release() {
        soundPool.release()
    }

    companion object {
        private const val MOVE_REPEAT_MS = 72L
        private const val SOFT_REPEAT_MS = 55L
        private const val JITTER = 0.06f
    }
}
