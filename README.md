# Russian Blocks

A classic Tetris-style block puzzle game for Android, written in Kotlin.

## Features

- All 7 standard tetrominoes (I, O, T, S, Z, J, L)
- Ghost piece showing where the block will land
- Next piece preview
- Score, level, and line tracking
- Increasing speed as you level up (every 10 lines)
- Wall-kick rotation
- Dark, modern UI
- Full-screen immersive gameplay

## Controls

| Action       | Gesture              |
|--------------|----------------------|
| Move left    | Swipe left           |
| Move right   | Swipe right          |
| Rotate       | Tap                  |
| Soft drop    | Swipe down (short)   |
| Hard drop    | Swipe down (long)    |

## Scoring

| Lines Cleared | Points (× level) |
|---------------|-------------------|
| 1             | 100               |
| 2             | 300               |
| 3             | 500               |
| 4 (Tetris!)   | 800               |

Soft drop: +1 per row. Hard drop: +2 per row.

## Building

1. Open the project in **Android Studio** (Arctic Fox or later)
2. Let Gradle sync complete
3. Click **Run** or press `Shift+F10`

Minimum SDK: Android 7.0 (API 24)
Target SDK: Android 14 (API 34)

## Project Structure

```
app/src/main/java/com/russianblocks/game/
├── MainActivity.kt     -- Activity with start screen and pause
├── GameView.kt         -- Custom View: rendering + touch input
├── GameEngine.kt       -- Core game state machine
├── Board.kt            -- 10×20 grid with collision & line clearing
└── Tetromino.kt        -- Piece shapes, rotation, spawning
```
