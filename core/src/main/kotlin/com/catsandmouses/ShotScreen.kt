package com.catsandmouses

/**
 * Экран для dev-снимка (`--shot-screen`). Только Desktop, в игровую логику не входит.
 */
enum class ShotScreen {
    LOADING, MENU, SETTINGS, CREDITS, GAME;

    companion object {
        /** Разбор кода флага; null — пусто/неизвестно. */
        fun fromCode(code: String?): ShotScreen? = when (code?.trim()?.lowercase()) {
            "loading" -> LOADING
            "menu" -> MENU
            "settings" -> SETTINGS
            "credits" -> CREDITS
            "game" -> GAME
            else -> null
        }
    }
}
