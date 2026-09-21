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
    /** Размер сердечка-жизни в HUD (на 25% меньше карты, чтобы 7 штук влезали в ряд). */
    const val HUD_HEART_SIZE = 36f
    /** Стартовое число жизней мыши (без подобранных сердечек). */
    const val START_LIVES = 1
    /** Максимум жизней, которые мышь накапливает. */
    const val MAX_LIVES = 7
    /** Длительность неуязвимости после потери жизни, сек. */
    const val INVULN_TIME = 1.5f
    /** Максимум сердечек, размещаемых на одной карте (лимит редактора). */
    const val MAX_HEARTS_PER_MAP = 3

    /** Длительность стартового отсчёта «3 → 2 → 1» перед началом игры, сек. */
    const val COUNTDOWN_TIME = 3f

    const val MAZE_WIDTH = MAZE_COLS * CELL_SIZE
    const val MAZE_HEIGHT = MAZE_ROWS * CELL_SIZE
    const val MAZE_OFFSET_X = (WORLD_WIDTH - MAZE_WIDTH) / 2f
    const val MAZE_OFFSET_Y = 0f
    const val VERSION = "1.0.60"

    // Preferences
    const val PREFS_NAME = "catsandmouses"
    const val KEY_BEST_SCORE = "bestScore"
    const val KEY_LAST_LEVEL = "lastLevel"
    const val KEY_LAST_SCORE = "lastScore"
    const val KEY_HAS_PROGRESS = "hasProgress"
    const val KEY_LIVES = "lives"

    // Манифест уровней (генерируется Gradle-задачей из assets/maps/)
    const val MAPS_MANIFEST = "maps/maps.json"

    // Шрифты (размеры кратны 16 — пиксель-перфект BoldPixels)
    const val FONT_SIZE_HUD = 16
    const val FONT_SIZE_BUTTON = 32
    const val FONT_SIZE_OVERLAY = 32
    const val FONT_SIZE_TITLE = 64
    /** Крупные цифры стартового отсчёта (3 → 2 → 1). */
    const val FONT_SIZE_COUNTDOWN = 192

    // Палитра текста (BoldPixels): кремовый основной, тёмно-коричневая обводка, акцент
    val FONT_COLOR_TEXT = Color(0.96f, 0.90f, 0.78f, 1f)      // #F5E6C8 — светло-кремовый
    val FONT_COLOR_OUTLINE = Color(0.23f, 0.14f, 0.07f, 1f)  // #3B2412 — тёмно-коричневая обводка
    val FONT_COLOR_ACCENT = Color(0.78f, 0.54f, 0.32f, 1f)   // #C78A52 — hover/акцент
}
