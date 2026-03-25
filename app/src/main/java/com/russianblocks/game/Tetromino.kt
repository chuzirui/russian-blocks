package com.russianblocks.game

import android.graphics.Color

enum class TetrominoShape(val color: Int, val blocks: Array<IntArray>) {

    /* ── 1 block ────────────────────────────────────────── */

    DOT(Color.rgb(200, 200, 215), arrayOf(
        intArrayOf(1)
    )),

    /* ── 2 blocks ───────────────────────────────────────── */

    I2(Color.rgb(255, 105, 180), arrayOf(
        intArrayOf(0, 0),
        intArrayOf(1, 1)
    )),

    SLASH2(Color.rgb(220, 180, 140), arrayOf(
        intArrayOf(0, 1),
        intArrayOf(1, 0)
    )),

    BSLASH2(Color.rgb(140, 180, 220), arrayOf(
        intArrayOf(1, 0),
        intArrayOf(0, 1)
    )),

    /* ── 3 blocks ───────────────────────────────────────── */

    I3(Color.rgb(180, 255, 0), arrayOf(
        intArrayOf(0, 0, 0),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 0)
    )),

    V3(Color.rgb(0, 190, 170), arrayOf(
        intArrayOf(1, 0),
        intArrayOf(1, 1)
    )),

    S3(Color.rgb(160, 230, 200), arrayOf(
        intArrayOf(0, 1),
        intArrayOf(1, 1)
    )),

    SLASH3(Color.rgb(255, 160, 100), arrayOf(
        intArrayOf(0, 0, 1),
        intArrayOf(0, 1, 0),
        intArrayOf(1, 0, 0)
    )),

    BSLASH3(Color.rgb(100, 160, 255), arrayOf(
        intArrayOf(1, 0, 0),
        intArrayOf(0, 1, 0),
        intArrayOf(0, 0, 1)
    )),

    /* ── 4 blocks (standard tetrominoes) ────────────────── */

    I(Color.rgb(0, 240, 240), arrayOf(
        intArrayOf(0, 0, 0, 0),
        intArrayOf(1, 1, 1, 1),
        intArrayOf(0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0)
    )),

    O(Color.rgb(240, 240, 0), arrayOf(
        intArrayOf(1, 1),
        intArrayOf(1, 1)
    )),

    T(Color.rgb(160, 0, 240), arrayOf(
        intArrayOf(0, 1, 0),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 0)
    )),

    S(Color.rgb(0, 240, 0), arrayOf(
        intArrayOf(0, 1, 1),
        intArrayOf(1, 1, 0),
        intArrayOf(0, 0, 0)
    )),

    Z(Color.rgb(240, 0, 0), arrayOf(
        intArrayOf(1, 1, 0),
        intArrayOf(0, 1, 1),
        intArrayOf(0, 0, 0)
    )),

    J(Color.rgb(0, 0, 240), arrayOf(
        intArrayOf(1, 0, 0),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 0)
    )),

    L(Color.rgb(240, 160, 0), arrayOf(
        intArrayOf(0, 0, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 0)
    )),

    /* ── 5 blocks (pentominoes) ─────────────────────────── */

    I5(Color.rgb(220, 20, 60), arrayOf(
        intArrayOf(0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0),
        intArrayOf(1, 1, 1, 1, 1),
        intArrayOf(0, 0, 0, 0, 0),
        intArrayOf(0, 0, 0, 0, 0)
    )),

    L5(Color.rgb(180, 80, 255), arrayOf(
        intArrayOf(1, 0, 0, 0),
        intArrayOf(1, 0, 0, 0),
        intArrayOf(1, 0, 0, 0),
        intArrayOf(1, 1, 0, 0)
    )),

    J5(Color.rgb(100, 80, 220), arrayOf(
        intArrayOf(0, 1, 0, 0),
        intArrayOf(0, 1, 0, 0),
        intArrayOf(0, 1, 0, 0),
        intArrayOf(1, 1, 0, 0)
    )),

    T5(Color.rgb(255, 127, 80), arrayOf(
        intArrayOf(1, 1, 1),
        intArrayOf(0, 1, 0),
        intArrayOf(0, 1, 0)
    )),

    S5(Color.rgb(200, 100, 180), arrayOf(
        intArrayOf(0, 1, 1),
        intArrayOf(0, 1, 0),
        intArrayOf(1, 1, 0)
    )),

    Z5(Color.rgb(100, 200, 160), arrayOf(
        intArrayOf(1, 1, 0),
        intArrayOf(0, 1, 0),
        intArrayOf(0, 1, 1)
    )),

    P5(Color.rgb(230, 140, 50), arrayOf(
        intArrayOf(1, 0, 0),
        intArrayOf(1, 1, 0),
        intArrayOf(1, 1, 0)
    )),

    Q5(Color.rgb(50, 180, 230), arrayOf(
        intArrayOf(0, 0, 1),
        intArrayOf(0, 1, 1),
        intArrayOf(0, 1, 1)
    )),

    U5(Color.rgb(255, 200, 60), arrayOf(
        intArrayOf(1, 0, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 0)
    )),

    /* ── 6 blocks (hexominoes) ──────────────────────────── */

    O6(Color.rgb(70, 140, 200), arrayOf(
        intArrayOf(1, 1, 0),
        intArrayOf(1, 1, 0),
        intArrayOf(1, 1, 0)
    ));

    companion object {
        private val shapes = values()
        fun random(): TetrominoShape = shapes.random()
    }
}

data class Tetromino(
    val shape: TetrominoShape,
    var x: Int,
    var y: Int,
    var blocks: Array<IntArray>
) {
    companion object {
        fun spawn(shape: TetrominoShape, boardWidth: Int): Tetromino {
            val blocks = shape.blocks.map { it.copyOf() }.toTypedArray()
            val startX = (boardWidth - blocks[0].size) / 2
            return Tetromino(shape, startX, 0, blocks)
        }
    }

    val width: Int get() = blocks[0].size
    val height: Int get() = blocks.size

    fun rotatedCW(): Array<IntArray> {
        val n = blocks.size
        val rotated = Array(n) { IntArray(n) }
        for (r in 0 until n) {
            for (c in 0 until n) {
                rotated[c][n - 1 - r] = blocks[r][c]
            }
        }
        return rotated
    }

    fun rotatedCCW(): Array<IntArray> {
        val n = blocks.size
        val rotated = Array(n) { IntArray(n) }
        for (r in 0 until n) {
            for (c in 0 until n) {
                rotated[n - 1 - c][r] = blocks[r][c]
            }
        }
        return rotated
    }
}
