package com.russianblocks.game

class Board(val width: Int = 12, val height: Int = 24) {

    /* Each cell is 0 (empty) or a packed ARGB color */
    val grid: Array<IntArray> = Array(height) { IntArray(width) }

    fun isValidPosition(blocks: Array<IntArray>, posX: Int, posY: Int): Boolean {
        for (r in blocks.indices) {
            for (c in blocks[r].indices) {
                if (blocks[r][c] == 0) continue
                val boardX = posX + c
                val boardY = posY + r
                if (boardX < 0 || boardX >= width || boardY >= height) return false
                if (boardY < 0) continue
                if (grid[boardY][boardX] != 0) return false
            }
        }
        return true
    }

    fun lock(piece: Tetromino) {
        for (r in piece.blocks.indices) {
            for (c in piece.blocks[r].indices) {
                if (piece.blocks[r][c] == 0) continue
                val boardY = piece.y + r
                val boardX = piece.x + c
                if (boardY in 0 until height && boardX in 0 until width) {
                    grid[boardY][boardX] = piece.shape.color
                }
            }
        }
    }

    /**
     * Clears completed rows and returns the number cleared.
     */
    fun clearLines(): Int {
        var cleared = 0
        var dest = height - 1
        for (src in height - 1 downTo 0) {
            if (grid[src].all { it != 0 }) {
                cleared++
                continue
            }
            if (dest != src) {
                grid[src].copyInto(grid[dest])
            }
            dest--
        }
        for (r in 0..dest) {
            grid[r].fill(0)
        }
        return cleared
    }

    /**
     * Per-column gravity: drops every block down to fill empty
     * cells below it. Returns true if anything moved.
     */
    fun applyGravity(): Boolean {
        var moved = false
        for (c in 0 until width) {
            var dest = height - 1
            for (r in height - 1 downTo 0) {
                if (grid[r][c] != 0) {
                    if (dest != r) {
                        grid[dest][c] = grid[r][c]
                        grid[r][c] = 0
                        moved = true
                    }
                    dest--
                }
            }
        }
        return moved
    }

    fun reset() {
        for (row in grid) row.fill(0)
    }
}
