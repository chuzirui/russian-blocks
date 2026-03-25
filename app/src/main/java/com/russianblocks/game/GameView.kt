package com.russianblocks.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), GameEngine.GameListener {

    private val engine = GameEngine(this)
    private val handler = Handler(Looper.getMainLooper())

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(30, 30, 42)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val ghostFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ghostStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val bgPaint = Paint().apply { color = Color.rgb(16, 16, 26) }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val panelPaint = Paint().apply { color = Color.rgb(24, 24, 38) }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(60, 60, 100)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val bevelPath = Path()

    private var cellSize = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    private var sideWidth = 0f

    var scoreCallback: ((score: Int, level: Int, lines: Int) -> Unit)? = null
    var gameOverCallback: ((finalScore: Int) -> Unit)? = null

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

    fun moveLeft()  = engine.moveLeft()
    fun moveRight() = engine.moveRight()
    fun rotateCW()  = engine.rotateCW()
    fun softDrop()  = engine.softDrop()
    fun hardDrop()  = engine.hardDrop()

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
    }

    /* ── Drawing ────────────────────────────────────────── */

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(15, 15, 25))

        drawBoard(canvas)
        drawGhost(canvas)
        drawCurrentPiece(canvas)
        drawSidePanel(canvas)
    }

    private fun drawBoard(canvas: Canvas) {
        val boardW = cellSize * engine.board.width
        val boardH = cellSize * engine.board.height

        canvas.drawRect(boardLeft, boardTop, boardLeft + boardW, boardTop + boardH, bgPaint)

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
        ghostFillPaint.color = baseColor and 0x00FFFFFF or 0x18000000
        ghostStrokePaint.color = baseColor and 0x00FFFFFF or 0x66000000
        for (r in piece.blocks.indices) {
            for (c in piece.blocks[r].indices) {
                if (piece.blocks[r][c] == 0) continue
                val x = boardLeft + (piece.x + c) * cellSize
                val y = boardTop + (engine.ghostY + r) * cellSize
                val inset = cellSize * 0.1f
                canvas.drawRect(x + inset, y + inset, x + cellSize - inset, y + cellSize - inset, ghostFillPaint)
                canvas.drawRect(x + inset, y + inset, x + cellSize - inset, y + cellSize - inset, ghostStrokePaint)
            }
        }
    }

    private fun drawCell(canvas: Canvas, x: Float, y: Float, color: Int) {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val bevel = (cellSize * 0.15f).coerceAtLeast(2f)
        val gap = 1f

        val left = x + gap
        val top = y + gap
        val right = x + cellSize - gap
        val bottom = y + cellSize - gap

        /* outer dark border */
        cellPaint.color = Color.rgb((r * 0.3f).toInt(), (g * 0.3f).toInt(), (b * 0.3f).toInt())
        canvas.drawRect(left, top, right, bottom, cellPaint)

        /* light bevel — top & left trapezoid */
        bevelPaint.color = Color.rgb(
            (r + (255 - r) * 0.45f).toInt().coerceAtMost(255),
            (g + (255 - g) * 0.45f).toInt().coerceAtMost(255),
            (b + (255 - b) * 0.45f).toInt().coerceAtMost(255)
        )
        bevelPath.reset()
        bevelPath.moveTo(left, top)
        bevelPath.lineTo(right, top)
        bevelPath.lineTo(right - bevel, top + bevel)
        bevelPath.lineTo(left + bevel, top + bevel)
        bevelPath.close()
        canvas.drawPath(bevelPath, bevelPaint)

        bevelPath.reset()
        bevelPath.moveTo(left, top)
        bevelPath.lineTo(left, bottom)
        bevelPath.lineTo(left + bevel, bottom - bevel)
        bevelPath.lineTo(left + bevel, top + bevel)
        bevelPath.close()
        canvas.drawPath(bevelPath, bevelPaint)

        /* dark bevel — bottom & right trapezoid */
        bevelPaint.color = Color.rgb((r * 0.45f).toInt(), (g * 0.45f).toInt(), (b * 0.45f).toInt())
        bevelPath.reset()
        bevelPath.moveTo(right, bottom)
        bevelPath.lineTo(left, bottom)
        bevelPath.lineTo(left + bevel, bottom - bevel)
        bevelPath.lineTo(right - bevel, bottom - bevel)
        bevelPath.close()
        canvas.drawPath(bevelPath, bevelPaint)

        bevelPath.reset()
        bevelPath.moveTo(right, bottom)
        bevelPath.lineTo(right, top)
        bevelPath.lineTo(right - bevel, top + bevel)
        bevelPath.lineTo(right - bevel, bottom - bevel)
        bevelPath.close()
        canvas.drawPath(bevelPath, bevelPaint)

        /* main face */
        cellPaint.color = color
        canvas.drawRect(left + bevel, top + bevel, right - bevel, bottom - bevel, cellPaint)

        /* top gloss gradient */
        val glossH = (bottom - top) * 0.4f
        shinePaint.shader = LinearGradient(
            left + bevel, top + bevel,
            left + bevel, top + bevel + glossH,
            Color.argb(100, 255, 255, 255),
            Color.argb(0, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(left + bevel, top + bevel, right - bevel, top + bevel + glossH, shinePaint)
        shinePaint.shader = null
    }

    private fun drawCellSmall(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val bevel = (size * 0.14f).coerceAtLeast(1.5f)
        val gap = 0.5f

        val left = x + gap
        val top = y + gap
        val right = x + size - gap
        val bottom = y + size - gap

        cellPaint.color = Color.rgb((r * 0.35f).toInt(), (g * 0.35f).toInt(), (b * 0.35f).toInt())
        canvas.drawRect(left, top, right, bottom, cellPaint)

        /* light top edge */
        bevelPaint.color = Color.rgb(
            (r + (255 - r) * 0.35f).toInt().coerceAtMost(255),
            (g + (255 - g) * 0.35f).toInt().coerceAtMost(255),
            (b + (255 - b) * 0.35f).toInt().coerceAtMost(255)
        )
        canvas.drawRect(left, top, right, top + bevel, bevelPaint)

        /* dark bottom edge */
        bevelPaint.color = Color.rgb((r * 0.5f).toInt(), (g * 0.5f).toInt(), (b * 0.5f).toInt())
        canvas.drawRect(left, bottom - bevel, right, bottom, bevelPaint)

        /* face */
        cellPaint.color = color
        canvas.drawRect(left + bevel, top + bevel, right - bevel, bottom - bevel, cellPaint)
    }

    private fun drawSidePanel(canvas: Canvas) {
        val boardW = cellSize * engine.board.width
        val panelLeft = boardLeft + boardW + 16f
        val panelTop = boardTop
        val previewSize = cellSize * 6

        canvas.drawRoundRect(
            panelLeft, panelTop, panelLeft + sideWidth, panelTop + previewSize,
            12f, 12f, panelPaint
        )
        canvas.drawRoundRect(
            panelLeft, panelTop, panelLeft + sideWidth, panelTop + previewSize,
            12f, 12f, borderPaint
        )

        textPaint.textAlign = Paint.Align.CENTER
        val cx = panelLeft + sideWidth / 2

        textPaint.color = Color.rgb(180, 180, 200)
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
        canvas.drawRoundRect(
            panelLeft, statsTop, panelLeft + sideWidth, statsTop + cellSize * 6,
            12f, 12f, panelPaint
        )
        canvas.drawRoundRect(
            panelLeft, statsTop, panelLeft + sideWidth, statsTop + cellSize * 6,
            12f, 12f, borderPaint
        )

        val lineH = cellSize * 1.1f
        var ty = statsTop + lineH

        textPaint.color = Color.rgb(120, 120, 160)
        canvas.drawText("SCORE", cx, ty, textPaint)
        ty += lineH * 0.8f
        textPaint.color = Color.WHITE
        canvas.drawText("${engine.score}", cx, ty, textPaint)

        ty += lineH
        textPaint.color = Color.rgb(120, 120, 160)
        canvas.drawText("LEVEL", cx, ty, textPaint)
        ty += lineH * 0.8f
        textPaint.color = Color.WHITE
        canvas.drawText("${engine.level}", cx, ty, textPaint)

        ty += lineH
        textPaint.color = Color.rgb(120, 120, 160)
        canvas.drawText("LINES", cx, ty, textPaint)
        ty += lineH * 0.8f
        textPaint.color = Color.WHITE
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
}
