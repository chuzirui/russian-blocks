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
        fun onBombCountChanged(bombs: Int)
        fun onHighScoreBroken(newHighScore: Int)
        fun onLineClear(lineCount: Int)
        fun onBombExplosion()
        fun onBombChainGravity()
        fun onBombCascadeStart()

        fun onPieceShifted()
        fun onPieceRotated()
        fun onSoftDropStep()
        fun onHardDropImpact(rowsDropped: Int)
        fun onPieceLockedNoClear()
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

    var highScore: Int = 0
    var bombs: Int = 0
    private var highScoreBrokenThisGame = false

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
        highScoreBrokenThisGame = false
        nextShape = TetrominoShape.random()
        spawnPiece()
        listener.onScoreChanged(score, level, totalLines)
        listener.onBombCountChanged(bombs)
    }

    /* ── Actions ────────────────────────────────────────── */

    fun moveLeft() {
        if (isGameOver || isPaused || bombAnimating) return
        val p = currentPiece ?: return
        if (board.isValidPosition(p.blocks, p.x - 1, p.y)) {
            p.x--
            updateGhost()
            listener.onPieceShifted()
            listener.onBoardUpdated()
        }
    }

    fun moveRight() {
        if (isGameOver || isPaused || bombAnimating) return
        val p = currentPiece ?: return
        if (board.isValidPosition(p.blocks, p.x + 1, p.y)) {
            p.x++
            updateGhost()
            listener.onPieceShifted()
            listener.onBoardUpdated()
        }
    }

    fun rotateCW() {
        if (isGameOver || isPaused || bombAnimating) return
        val p = currentPiece ?: return
        val rotated = p.rotatedCW()
        if (tryRotateKeepLeftEdge(p, rotated)) {
            updateGhost()
            listener.onPieceRotated()
            listener.onBoardUpdated()
        }
    }

    fun softDrop() {
        if (isGameOver || isPaused || bombAnimating) return
        val p = currentPiece ?: return
        if (board.isValidPosition(p.blocks, p.x, p.y + 1)) {
            p.y++
            score += 1
            checkHighScore()
            listener.onSoftDropStep()
            listener.onScoreChanged(score, level, totalLines)
            listener.onBoardUpdated()
        }
    }

    fun hardDrop() {
        if (isGameOver || isPaused || bombAnimating) return
        val p = currentPiece ?: return
        var rows = 0
        while (board.isValidPosition(p.blocks, p.x, p.y + 1)) {
            p.y++
            rows++
        }
        score += rows * 2
        checkHighScore()
        lockAndAdvance(fromHardDrop = true, hardDropRows = rows)
    }

    /**
     * Uses a bomb: removes all blocks of the most common color,
     * applies gravity, and clears any resulting lines.
     */
    private var bombAnimating = false

    fun useBomb() {
        if (isGameOver || isPaused || bombAnimating) return
        if (bombs <= 0) return
        val color = board.getMostCommonColor()
        if (color == 0) return

        bombs--
        listener.onBombCountChanged(bombs)

        board.removeColor(color)
        listener.onBombExplosion()
        listener.onBoardUpdated()
        bombAnimating = true
        listener.onBombCascadeStart()
    }

    /** Gravity phase of bomb chain (blocks fall). */
    fun bombCascadeGravityPhase() {
        board.applyGravity()
        listener.onBoardUpdated()
        listener.onBombChainGravity()
    }

    /**
     * Line-clear phase after gravity. Returns true if lines were cleared
     * (another gravity+clear cycle may follow).
     */
    fun bombCascadeClearPhase(): Boolean {
        val cleared = board.clearLines()
        if (cleared > 0) {
            totalLines += cleared
            score += lineScore(cleared) * level
            level = (totalLines / 10) + 1
            checkHighScore()
            listener.onLineClear(cleared)
            listener.onScoreChanged(score, level, totalLines)
            listener.onBoardUpdated()
            return true
        }
        bombAnimating = false
        checkHighScore()
        listener.onScoreChanged(score, level, totalLines)
        return false
    }

    /** Called by the game loop at each drop interval */
    fun tick() {
        if (isGameOver || isPaused || bombAnimating) return
        val p = currentPiece ?: return
        if (board.isValidPosition(p.blocks, p.x, p.y + 1)) {
            p.y++
            listener.onBoardUpdated()
        } else {
            lockAndAdvance(fromHardDrop = false)
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

    private fun lockAndAdvance(fromHardDrop: Boolean = false, hardDropRows: Int = 0) {
        val p = currentPiece ?: return
        board.lock(p)

        if (fromHardDrop) listener.onHardDropImpact(hardDropRows)

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
            listener.onLineClear(totalCleared)
        } else if (!fromHardDrop) {
            listener.onPieceLockedNoClear()
        }
        checkHighScore()
        listener.onScoreChanged(score, level, totalLines)
        spawnPiece()
    }

    private fun checkHighScore() {
        if (!highScoreBrokenThisGame && score > highScore && highScore > 0) {
            highScoreBrokenThisGame = true
            bombs++
            listener.onBombCountChanged(bombs)
            listener.onHighScoreBroken(score)
        }
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

    /**
     * Keeps the board column of the leftmost filled cell fixed; only vertical
     * nudges are tried if the anchored position collides (no horizontal slide).
     */
    private fun tryRotateKeepLeftEdge(piece: Tetromino, rotated: Array<IntArray>): Boolean {
        val leftBoardCol = piece.x + leftmostFilledCol(piece.blocks)
        val minColRot = leftmostFilledCol(rotated)
        val newX = leftBoardCol - minColRot
        for (dy in intArrayOf(0, -1, 1, -2, 2)) {
            if (board.isValidPosition(rotated, newX, piece.y + dy)) {
                piece.x = newX
                piece.y += dy
                piece.blocks = rotated
                return true
            }
        }
        return false
    }

    private fun leftmostFilledCol(blocks: Array<IntArray>): Int {
        var minC = Int.MAX_VALUE
        for (r in blocks.indices) {
            for (c in blocks[r].indices) {
                if (blocks[r][c] != 0) minC = minOf(minC, c)
            }
        }
        return if (minC == Int.MAX_VALUE) 0 else minC
    }
}
