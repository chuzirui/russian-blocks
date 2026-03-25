package com.russianblocks.game

/**
 * Core game state machine.
 * Tick-driven: the view calls [tick] at the current drop interval.
 */
class GameEngine(private val listener: GameListener) {

    interface GameListener {
        fun onScoreChanged(score: Int, level: Int, lines: Int)
        fun onGameOver(finalScore: Int)
        fun onBoardUpdated()
    }

    val board = Board()
    var currentPiece: Tetromino? = null
        private set
    var nextShape: TetrominoShape = TetrominoShape.random()
        private set
    var ghostY: Int = 0
        private set

    var score: Int = 0; private set
    var level: Int = 1; private set
    var totalLines: Int = 0; private set
    var isGameOver: Boolean = false; private set
    var isPaused: Boolean = false

    /** Drop interval in ms — speeds up with level */
    val dropInterval: Long
        get() = (1000L - (level - 1) * 80L).coerceAtLeast(100L)

    fun start() {
        board.reset()
        score = 0
        level = 1
        totalLines = 0
        isGameOver = false
        isPaused = false
        nextShape = TetrominoShape.random()
        spawnPiece()
        listener.onScoreChanged(score, level, totalLines)
    }

    /* ── Actions ────────────────────────────────────────── */

    fun moveLeft() {
        if (isGameOver || isPaused) return
        val p = currentPiece ?: return
        if (board.isValidPosition(p.blocks, p.x - 1, p.y)) {
            p.x--
            updateGhost()
            listener.onBoardUpdated()
        }
    }

    fun moveRight() {
        if (isGameOver || isPaused) return
        val p = currentPiece ?: return
        if (board.isValidPosition(p.blocks, p.x + 1, p.y)) {
            p.x++
            updateGhost()
            listener.onBoardUpdated()
        }
    }

    fun rotateCW() {
        if (isGameOver || isPaused) return
        val p = currentPiece ?: return
        val rotated = p.rotatedCW()
        if (tryWallKick(p, rotated)) {
            p.blocks = rotated
            updateGhost()
            listener.onBoardUpdated()
        }
    }

    fun softDrop() {
        if (isGameOver || isPaused) return
        val p = currentPiece ?: return
        if (board.isValidPosition(p.blocks, p.x, p.y + 1)) {
            p.y++
            score += 1
            listener.onScoreChanged(score, level, totalLines)
            listener.onBoardUpdated()
        }
    }

    fun hardDrop() {
        if (isGameOver || isPaused) return
        val p = currentPiece ?: return
        var rows = 0
        while (board.isValidPosition(p.blocks, p.x, p.y + 1)) {
            p.y++
            rows++
        }
        score += rows * 2
        lockAndAdvance()
    }

    /** Called by the game loop at each drop interval */
    fun tick() {
        if (isGameOver || isPaused) return
        val p = currentPiece ?: return
        if (board.isValidPosition(p.blocks, p.x, p.y + 1)) {
            p.y++
            listener.onBoardUpdated()
        } else {
            lockAndAdvance()
        }
    }

    /* ── Internals ──────────────────────────────────────── */

    private fun spawnPiece() {
        val shape = nextShape
        nextShape = TetrominoShape.random()
        val piece = Tetromino.spawn(shape, board.width)
        if (!board.isValidPosition(piece.blocks, piece.x, piece.y)) {
            isGameOver = true
            listener.onGameOver(score)
            return
        }
        currentPiece = piece
        updateGhost()
        listener.onBoardUpdated()
    }

    private fun lockAndAdvance() {
        val p = currentPiece ?: return
        board.lock(p)

        /* Only apply gravity when lines are actually cleared */
        var totalCleared = 0
        var cleared = board.clearLines()
        while (cleared > 0) {
            totalCleared += cleared
            board.applyGravity()
            cleared = board.clearLines()
        }

        if (totalCleared > 0) {
            totalLines += totalCleared
            score += lineScore(totalCleared) * level
            level = (totalLines / 10) + 1
        }
        listener.onScoreChanged(score, level, totalLines)
        spawnPiece()
    }

    private fun lineScore(lines: Int): Int = when {
        lines >= 6 -> 1200
        lines == 5 -> 1000
        lines == 4 -> 800
        lines == 3 -> 500
        lines == 2 -> 300
        lines == 1 -> 100
        else -> 0
    }

    private fun updateGhost() {
        val p = currentPiece ?: return
        var gy = p.y
        while (board.isValidPosition(p.blocks, p.x, gy + 1)) gy++
        ghostY = gy
    }

    private fun tryWallKick(piece: Tetromino, rotated: Array<IntArray>): Boolean {
        val maxKick = rotated.size / 2 + 1
        for (dx in 0..maxKick) {
            for (offset in if (dx == 0) intArrayOf(0) else intArrayOf(-dx, dx)) {
                if (board.isValidPosition(rotated, piece.x + offset, piece.y)) {
                    piece.x += offset
                    return true
                }
            }
        }
        return false
    }
}
