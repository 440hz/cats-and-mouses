package com.catsandmouses

object Constants {
    const val WORLD_WIDTH = 1280f
    const val WORLD_HEIGHT = 720f
    const val CELL_SIZE = 40f
    const val MAZE_COLS = 28
    const val MAZE_ROWS = 16

    // UI / HUD (объявлены до MAZE_OFFSET_Y, чтобы использовать в нём)
    const val HUD_HEIGHT = 56f
    const val BUTTON_WIDTH = 360f
    const val BUTTON_HEIGHT = 170f
    const val HEART_SIZE = 48f
    const val MAX_LIVES = 3

    const val MAZE_WIDTH = MAZE_COLS * CELL_SIZE
    const val MAZE_HEIGHT = MAZE_ROWS * CELL_SIZE
    const val MAZE_OFFSET_X = (WORLD_WIDTH - MAZE_WIDTH) / 2f
    const val MAZE_OFFSET_Y = (WORLD_HEIGHT - MAZE_HEIGHT - HUD_HEIGHT) / 2f + HUD_HEIGHT
    const val VERSION = "1.0.33"

    // Preferences
    const val PREFS_NAME = "catsandmouses"
    const val KEY_BEST_SCORE = "bestScore"
    const val KEY_LAST_LEVEL = "lastLevel"
    const val KEY_LAST_SCORE = "lastScore"
    const val KEY_HAS_PROGRESS = "hasProgress"

    // Шрифты
    const val FONT_SIZE_HUD = 20
    const val FONT_SIZE_BUTTON = 28
    const val FONT_SIZE_OVERLAY = 32
    const val FONT_SIZE_TITLE = 64
}
