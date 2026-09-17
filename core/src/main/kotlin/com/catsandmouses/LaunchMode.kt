package com.catsandmouses

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx

/**
 * Режим запуска — отображается рядом с версией:
 * `(d)` — dev (`lwjgl3:run`), `(r)` — дистрибутив (`runDist`/`desktop-run.bat`),
 * `(i)` — конфигурация IDEA «Run Idea», `(a)` — Android.
 */
object LaunchMode {

    /** Суффикс вида `(d)`/`(r)`/`(i)`/`(a)` или пустая строка. */
    val suffix: String
        get() = resolveMode()?.let { "($it)" } ?: ""

    private fun resolveMode(): String? {
        // Явный признак (задаётся в конфигурации IDEA «Run Idea» как -Dlaunch.mode=i)
        System.getProperty("launch.mode")?.takeIf { it.isNotBlank() }?.let { return it }

        // Android
        if (Gdx.app?.type == Application.ApplicationType.Android) return "a"

        // Классы лежат в jar → запущен собранный дистрибутив (Run Dist)
        val location = LaunchMode::class.java.protectionDomain?.codeSource?.location?.toString()
        return if (location != null && location.endsWith(".jar", ignoreCase = true)) "r" else "d"
    }
}
