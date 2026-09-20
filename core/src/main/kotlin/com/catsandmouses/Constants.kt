package com.catsandmouses

import com.badlogic.gdx.graphics.Color

object Constants {
    const val WORLD_WIDTH = 1280f
    const val WORLD_HEIGHT = 720f
    const val CELL_SIZE = 40f
    const val MAZE_COLS = 32
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
    const val MAZE_OFFSET_Y = 0f
    const val VERSION = "1.0.51"

    // Preferences
    const val PREFS_NAME = "catsandmouses"
    const val KEY_BEST_SCORE = "bestScore"
    const val KEY_LAST_LEVEL = "lastLevel"
    const val KEY_LAST_SCORE = "lastScore"
    const val KEY_HAS_PROGRESS = "hasProgress"

    // Манифест уровней (генерируется Gradle-задачей из assets/maps/)
    const val MAPS_MANIFEST = "maps/maps.json"

    // Шрифты (размеры кратны 16 — пиксель-перфект BoldPixels)
    const val FONT_SIZE_HUD = 16
    const val FONT_SIZE_BUTTON = 32
    const val FONT_SIZE_OVERLAY = 32
    const val FONT_SIZE_TITLE = 64

    // Палитра текста (BoldPixels): кремовый основной, тёмно-коричневая обводка, акцент
    val FONT_COLOR_TEXT = Color(0.96f, 0.90f, 0.78f, 1f)      // #F5E6C8 — светло-кремовый
    val FONT_COLOR_OUTLINE = Color(0.23f, 0.14f, 0.07f, 1f)  // #3B2412 — тёмно-коричневая обводка
    val FONT_COLOR_ACCENT = Color(0.78f, 0.54f, 0.32f, 1f)   // #C78A52 — hover/акцент
}
