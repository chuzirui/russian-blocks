package com.russianblocks.game

class Board(val width: Int = 10, val height: Int = 21) {

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
     * Connected-component gravity: finds groups of connected blocks
     * and drops each group as a whole unit only if nothing supports it.
     * Repeats until everything is settled. Returns true if anything moved.
     */
    fun applyGravity(): Boolean {
        var anyMoved = false
        var moved: Boolean
        do {
            moved = false
            val components = findComponents()
            for (comp in components.sortedByDescending { it.maxOf { (r, _) -> r } }) {
                val cells = comp.toSet()
                var minDrop = height
                for ((r, c) in cells) {
                    var d = 0
                    var nr = r + 1
                    while (nr < height && (grid[nr][c] == 0 || (nr to c) in cells)) {
                        d++
                        nr++
                    }
                    minDrop = minOf(minDrop, d)
                }
                if (minDrop > 0) {
                    val saved = comp.map { (r, c) -> Triple(r, c, grid[r][c]) }
                    for ((r, c, _) in saved) grid[r][c] = 0
                    for ((r, c, color) in saved) grid[r + minDrop][c] = color
                    moved = true
                    anyMoved = true
                }
            }
        } while (moved)
        return anyMoved
    }

    private fun findComponents(): List<List<Pair<Int, Int>>> {
        val visited = Array(height) { BooleanArray(width) }
        val result = mutableListOf<List<Pair<Int, Int>>>()
        for (r in 0 until height) {
            for (c in 0 until width) {
                if (grid[r][c] != 0 && !visited[r][c]) {
                    val comp = mutableListOf<Pair<Int, Int>>()
                    val queue = ArrayDeque<Pair<Int, Int>>()
                    queue.add(r to c)
                    visited[r][c] = true
                    while (queue.isNotEmpty()) {
                        val (cr, cc) = queue.removeFirst()
                        comp.add(cr to cc)
                        for ((dr, dc) in arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                            val nr = cr + dr
                            val nc = cc + dc
                            if (nr in 0 until height && nc in 0 until width
                                && grid[nr][nc] != 0 && !visited[nr][nc]) {
                                visited[nr][nc] = true
                                queue.add(nr to nc)
                            }
                        }
                    }
                    result.add(comp)
                }
            }
        }
        return result
    }

    /**
     * Returns the most common non-empty color on the board, or 0 if empty.
     */
    fun getMostCommonColor(): Int {
        val counts = mutableMapOf<Int, Int>()
        for (r in 0 until height) {
            for (c in 0 until width) {
                val color = grid[r][c]
                if (color != 0) counts[color] = (counts[color] ?: 0) + 1
            }
        }
        return counts.maxByOrNull { it.value }?.key ?: 0
    }

    /**
     * Removes all cells of the given color. Returns count removed.
     */
    fun removeColor(color: Int): Int {
        var count = 0
        for (r in 0 until height) {
            for (c in 0 until width) {
                if (grid[r][c] == color) {
                    grid[r][c] = 0
                    count++
                }
            }
        }
        return count
    }

    fun reset() {
        for (row in grid) row.fill(0)
    }
}
