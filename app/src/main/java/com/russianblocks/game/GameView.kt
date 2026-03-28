package com.russianblocks.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View.MeasureSpec
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), GameEngine.GameListener {

    private val engine = GameEngine(this)
    private val handler = Handler(Looper.getMainLooper())

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val ghostFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ghostStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        typeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)
            ?: Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private var colorGameCanvas = 0
    private var colorBoardBg = 0
    private var colorGameGrid = 0
    private var colorBoardFrame = 0
    private var colorHudPanel = 0
    private var colorHudStroke = 0
    private var colorHudLabel = 0
    private var colorHudValue = 0
    private var colorHudMuted = 0
    private var colorHudGold = 0
    private var colorGhostOutline = 0

    private var cellSize = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    private var sideWidth = 0f

    init {
        resolveGameColors()
    }

    /** Called after [onSizeChanged] so the host can align overlays (e.g. bomb) with the HUD column. */
    var bombLayoutListener: (() -> Unit)? = null

    var scoreCallback: ((score: Int, level: Int, lines: Int) -> Unit)? = null
    var gameOverCallback: ((finalScore: Int) -> Unit)? = null
    var bombCountCallback: ((bombs: Int) -> Unit)? = null
    var highScoreBrokenCallback: ((newHighScore: Int) -> Unit)? = null
    var soundManager: GameSoundManager? = null

    private companion object {
        private const val BOMB_MS_GRAVITY_TO_CLEAR = 580L
        private const val BOMB_MS_AFTER_CLEAR_NEXT = 680L
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            engine.tick()
            if (!engine.isGameOver && !engine.isPaused) {
                handler.postDelayed(this, engine.dropInterval)
            }
        }
    }

    /* ── Touch handling ─────────────────────────────────── */

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchMoved = false
    private var lastMoveX = 0f
    private val swipeThreshold = 30f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (engine.isGameOver) return true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                lastMoveX = event.x
                touchMoved = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastMoveX
                val dy = event.y - touchStartY
                if (Math.abs(dx) > swipeThreshold) {
                    if (dx > 0) engine.moveRight() else engine.moveLeft()
                    lastMoveX = event.x
                    touchMoved = true
                }
                if (dy > swipeThreshold * 4) {
                    engine.hardDrop()
                    touchStartY = event.y
                    touchMoved = true
                } else if (dy > swipeThreshold * 2) {
                    engine.softDrop()
                    touchStartY = event.y
                    touchMoved = true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!touchMoved) {
                    engine.rotateCW()
                }
            }
        }
        return true
    }

    /* ── Lifecycle ──────────────────────────────────────── */

    fun startGame() {
        engine.start()
        handler.removeCallbacks(tickRunnable)
        handler.postDelayed(tickRunnable, engine.dropInterval)
    }

    fun pauseGame() {
        engine.isPaused = true
        handler.removeCallbacks(tickRunnable)
    }

    fun resumeGame() {
        if (engine.isGameOver) return
        engine.isPaused = false
        handler.removeCallbacks(tickRunnable)
        handler.postDelayed(tickRunnable, engine.dropInterval)
    }

    val isGameOver get() = engine.isGameOver
    val isPaused get() = engine.isPaused

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        resolveGameColors()
    }

    private fun resolveGameColors() {
        colorGameCanvas = ContextCompat.getColor(context, R.color.ds_game_canvas)
        colorBoardBg = ContextCompat.getColor(context, R.color.ds_game_board_bg)
        colorGameGrid = ContextCompat.getColor(context, R.color.ds_game_grid)
        colorBoardFrame = ContextCompat.getColor(context, R.color.ds_game_board_frame)
        colorHudPanel = ContextCompat.getColor(context, R.color.ds_hud_panel)
        colorHudStroke = ContextCompat.getColor(context, R.color.ds_hud_stroke)
        colorHudLabel = ContextCompat.getColor(context, R.color.ds_hud_label)
        colorHudValue = ContextCompat.getColor(context, R.color.ds_hud_value)
        colorHudMuted = ContextCompat.getColor(context, R.color.ds_hud_muted)
        colorHudGold = ContextCompat.getColor(context, R.color.ds_hud_gold)
        colorGhostOutline = ContextCompat.getColor(context, R.color.ds_ghost_outline)
        gridPaint.color = colorGameGrid
        panelPaint.color = colorHudPanel
    }

    fun moveLeft()  = engine.moveLeft()
    fun moveRight() = engine.moveRight()
    fun rotateCW()  = engine.rotateCW()
    fun softDrop()  = engine.softDrop()
    fun hardDrop()  = engine.hardDrop()
    fun useBomb()   = engine.useBomb()

    var highScore: Int
        get() = engine.highScore
        set(value) { engine.highScore = value }

    var bombs: Int
        get() = engine.bombs
        set(value) { engine.bombs = value }

    /* ── Layout ─────────────────────────────────────────── */

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = 8f
        val minSidePanel = 100f

        val cellByHeight = (h - padding * 2) / engine.board.height
        val cellByWidth = (w - padding * 2 - minSidePanel) / engine.board.width
        cellSize = cellByHeight.coerceAtMost(cellByWidth)

        val boardW = cellSize * engine.board.width
        val boardH = cellSize * engine.board.height

        boardLeft = padding
        boardTop = (h - boardH) / 2f
        sideWidth = w - boardLeft - boardW - padding * 2

        textPaint.textSize = (cellSize * 0.55f).coerceAtLeast(24f)

        ghostStrokePaint.strokeWidth = (cellSize * 0.08f).coerceIn(2.5f, 5f)

        gridPaint.strokeWidth = (cellSize * 0.035f).coerceIn(1.5f, 3f)
        borderPaint.strokeWidth = (cellSize * 0.05f).coerceIn(2f, 4f)

        bombLayoutListener?.invoke()
    }

    /**
     * Places [button] in the HUD column: same width as the NEXT / SCORE rounded rects, bottom aligned with the playfield.
     */
    fun layoutBombButton(button: View) {
        if (width <= 0 || height <= 0 || cellSize <= 0f) return
        val boardW = cellSize * engine.board.width
        val boardH = cellSize * engine.board.height
        val panelLeft = boardLeft + boardW + 16f
        val panelW = sideWidth.toInt().coerceAtLeast(1)
        val boardBottom = boardTop + boardH

        val lp = button.layoutParams as? FrameLayout.LayoutParams ?: return
        lp.leftMargin = panelLeft.toInt()
        lp.width = panelW
        lp.height = FrameLayout.LayoutParams.WRAP_CONTENT
        lp.gravity = Gravity.TOP or Gravity.START

        button.measure(
            MeasureSpec.makeMeasureSpec(panelW, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val bombH = button.measuredHeight
        lp.topMargin = (boardBottom - bombH).toInt().coerceAtLeast(0)

        button.layoutParams = lp
    }

    /* ── Drawing ────────────────────────────────────────── */

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(colorGameCanvas)

        drawBoard(canvas)
        drawGhost(canvas)
        drawCurrentPiece(canvas)
        drawSidePanel(canvas)
    }

    private fun drawBoard(canvas: Canvas) {
        val boardW = cellSize * engine.board.width
        val boardH = cellSize * engine.board.height

        cellPaint.color = colorBoardBg
        canvas.drawRect(boardLeft, boardTop, boardLeft + boardW, boardTop + boardH, cellPaint)

        for (r in 0 until engine.board.height) {
            for (c in 0 until engine.board.width) {
                val x = boardLeft + c * cellSize
                val y = boardTop + r * cellSize
                val color = engine.board.grid[r][c]
                if (color != 0) {
                    drawCell(canvas, x, y, color)
                }
                canvas.drawRect(x, y, x + cellSize, y + cellSize, gridPaint)
            }
        }

        borderPaint.color = colorBoardFrame
        canvas.drawRect(boardLeft, boardTop, boardLeft + boardW, boardTop + boardH, borderPaint)
    }

    private fun drawCurrentPiece(canvas: Canvas) {
        val piece = engine.currentPiece ?: return
        for (r in piece.blocks.indices) {
            for (c in piece.blocks[r].indices) {
                if (piece.blocks[r][c] == 0) continue
                val x = boardLeft + (piece.x + c) * cellSize
                val y = boardTop + (piece.y + r) * cellSize
                drawCell(canvas, x, y, piece.shape.color)
            }
        }
    }

    private fun drawGhost(canvas: Canvas) {
        val piece = engine.currentPiece ?: return
        if (engine.ghostY == piece.y) return
        val baseColor = piece.shape.color
        val r = Color.red(baseColor)
        val g = Color.green(baseColor)
        val b = Color.blue(baseColor)
        /* Filled silhouette — ~38% alpha */
        ghostFillPaint.color = Color.argb(98, r, g, b)
        ghostStrokePaint.color = colorGhostOutline
        val inset = cellSize * 0.08f
        for (row in piece.blocks.indices) {
            for (col in piece.blocks[row].indices) {
                if (piece.blocks[row][col] == 0) continue
                val x = boardLeft + (piece.x + col) * cellSize
                val y = boardTop + (engine.ghostY + row) * cellSize
                canvas.drawRect(x + inset, y + inset, x + cellSize - inset, y + cellSize - inset, ghostFillPaint)
                canvas.drawRect(x + inset, y + inset, x + cellSize - inset, y + cellSize - inset, ghostStrokePaint)
            }
        }
    }

    private fun drawCell(canvas: Canvas, x: Float, y: Float, color: Int) {
        val gap = 1.5f
        val left = x + gap
        val top = y + gap
        val right = x + cellSize - gap
        val bottom = y + cellSize - gap

        cellPaint.color = color
        canvas.drawRect(left, top, right, bottom, cellPaint)

        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        borderPaint.color = Color.rgb(
            (r * 0.55f).toInt(), (g * 0.55f).toInt(), (b * 0.55f).toInt()
        )
        canvas.drawRect(left, top, right, bottom, borderPaint)
    }

    private fun drawCellSmall(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
        val gap = 1f
        val left = x + gap
        val top = y + gap
        val right = x + size - gap
        val bottom = y + size - gap

        cellPaint.color = color
        canvas.drawRect(left, top, right, bottom, cellPaint)

        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        borderPaint.color = Color.rgb(
            (r * 0.55f).toInt(), (g * 0.55f).toInt(), (b * 0.55f).toInt()
        )
        canvas.drawRect(left, top, right, bottom, borderPaint)
    }

    private fun drawSidePanel(canvas: Canvas) {
        val boardW = cellSize * engine.board.width
        val panelLeft = boardLeft + boardW + 16f
        val panelTop = boardTop
        val previewSize = cellSize * 6
        val corner = (cellSize * 0.22f).coerceIn(10f, 16f)

        canvas.drawRoundRect(
            panelLeft, panelTop, panelLeft + sideWidth, panelTop + previewSize,
            corner, corner, panelPaint
        )
        borderPaint.color = colorHudStroke
        canvas.drawRoundRect(
            panelLeft, panelTop, panelLeft + sideWidth, panelTop + previewSize,
            corner, corner, borderPaint
        )

        textPaint.textAlign = Paint.Align.CENTER
        val cx = panelLeft + sideWidth / 2

        textPaint.color = colorHudMuted
        canvas.drawText("NEXT", cx, panelTop + cellSize * 0.8f, textPaint)

        val nextBlocks = engine.nextShape.blocks
        val gridN = nextBlocks.size
        val maxPreviewCell = ((previewSize - cellSize * 1.6f) / gridN).coerceAtMost(sideWidth * 0.8f / gridN)
        val previewCellSize = maxPreviewCell.coerceAtMost(cellSize * 0.85f)
        val nextW = gridN * previewCellSize
        val nextH = gridN * previewCellSize
        val nextLeft = cx - nextW / 2
        val nextTop = panelTop + cellSize * 1.2f + (previewSize - cellSize * 1.8f - nextH) / 2

        for (r in nextBlocks.indices) {
            for (c in nextBlocks[r].indices) {
                if (nextBlocks[r][c] == 0) continue
                val bx = nextLeft + c * previewCellSize
                val by = nextTop + r * previewCellSize
                drawCellSmall(canvas, bx, by, previewCellSize, engine.nextShape.color)
            }
        }

        val statsTop = panelTop + previewSize + 24f
        val statsH = cellSize * 7.4f
        canvas.drawRoundRect(
            panelLeft, statsTop, panelLeft + sideWidth, statsTop + statsH,
            corner, corner, panelPaint
        )
        borderPaint.color = colorHudStroke
        canvas.drawRoundRect(
            panelLeft, statsTop, panelLeft + sideWidth, statsTop + statsH,
            corner, corner, borderPaint
        )

        val lineH = cellSize * 0.95f
        var ty = statsTop + lineH

        textPaint.color = colorHudLabel
        canvas.drawText("SCORE", cx, ty, textPaint)
        ty += lineH * 0.75f
        textPaint.color = colorHudValue
        canvas.drawText("${engine.score}", cx, ty, textPaint)

        ty += lineH
        textPaint.color = colorHudLabel
        canvas.drawText("BEST", cx, ty, textPaint)
        ty += lineH * 0.75f
        textPaint.color = if (engine.score > engine.highScore && engine.highScore > 0)
            colorHudGold else colorHudMuted
        canvas.drawText("${maxOf(engine.highScore, engine.score)}", cx, ty, textPaint)

        ty += lineH
        textPaint.color = colorHudLabel
        canvas.drawText("LEVEL", cx, ty, textPaint)
        ty += lineH * 0.75f
        textPaint.color = colorHudValue
        canvas.drawText("${engine.level}", cx, ty, textPaint)

        ty += lineH
        textPaint.color = colorHudLabel
        canvas.drawText("LINES", cx, ty, textPaint)
        ty += lineH * 0.75f
        textPaint.color = colorHudValue
        canvas.drawText("${engine.totalLines}", cx, ty, textPaint)

    }

    /* ── Engine callbacks ───────────────────────────────── */

    override fun onScoreChanged(score: Int, level: Int, lines: Int) {
        scoreCallback?.invoke(score, level, lines)
        postInvalidate()
    }

    override fun onGameOver(finalScore: Int) {
        handler.removeCallbacks(tickRunnable)
        gameOverCallback?.invoke(finalScore)
        postInvalidate()
    }

    override fun onBoardUpdated() {
        postInvalidate()
    }

    override fun onBombCountChanged(bombs: Int) {
        bombCountCallback?.invoke(bombs)
        postInvalidate()
    }

    override fun onHighScoreBroken(newHighScore: Int) {
        highScoreBrokenCallback?.invoke(newHighScore)
        postInvalidate()
    }

    override fun onLineClear(lineCount: Int) {
        soundManager?.playLineClear(lineCount)
        postInvalidate()
    }

    override fun onBombExplosion() {
        soundManager?.playBombExplosion()
        postInvalidate()
    }

    override fun onBombChainGravity() {
        soundManager?.playChainGravity()
        postInvalidate()
    }

    override fun onPieceShifted() {
        soundManager?.playMove()
    }

    override fun onPieceRotated() {
        soundManager?.playRotate()
    }

    override fun onSoftDropStep() {
        soundManager?.playSoftDrop()
    }

    override fun onHardDropImpact(rowsDropped: Int) {
        soundManager?.playHardDrop(rowsDropped)
    }

    override fun onPieceLockedNoClear() {
        soundManager?.playLock()
    }

    override fun onBombCascadeStart() {
        runBombCascadeStep()
    }

    private fun runBombCascadeStep() {
        engine.bombCascadeGravityPhase()
        handler.postDelayed({
            val needContinue = engine.bombCascadeClearPhase()
            if (needContinue) {
                handler.postDelayed({ runBombCascadeStep() }, BOMB_MS_AFTER_CLEAR_NEXT)
            }
        }, BOMB_MS_GRAVITY_TO_CLEAR)
    }
}
