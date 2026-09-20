package com.catsandmouses.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.catsandmouses.MainGame

class Lwjgl3Launcher {
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val startMap = parseStartMap(args)
            val debugLogs = parseDebugLogs(args)
            val aiModePort = parseAiModePort(args)
            val themeId = parseTheme(args)
            val editorMode = parseEditor(args)
            val assetsDir = parseAssetsDir(args)
            val game = MainGame(
                startMap = startMap ?: "1/1",
                startMapExplicit = startMap != null,
                debugLogs = debugLogs,
                aiModePort = aiModePort,
                themeId = themeId,
                editorFactory = if (editorMode) { mg -> EditorScreen(mg, assetsDir) } else null
            )
            Lwjgl3Application(game, Lwjgl3ApplicationConfiguration().apply {
                setTitle(if (editorMode) "CatsAndMouses — редактор карт" else "CatsAndMouses")
                setWindowedMode(1280, 720)
            })
        }

        /**
         * Отладочный запуск с указанной карты (без меню). null — параметр не задан.
         * Примеры:
         *   --start-map 1/2
         *   --start-map final
         *   --start-level 1/4  (обратная совместимость)
         */
        private fun parseStartMap(args: Array<String>): String? {
            for (i in args.indices) {
                if ((args[i] == "--start-map" || args[i] == "--start-level") && i + 1 < args.size) {
                    return args[i + 1]
                }
                if (args[i].startsWith("--start-map=") || args[i].startsWith("--start-level=")) {
                    return args[i].substringAfter("=")
                }
            }
            return null
        }

        /**
         * Проверяет флаг включения логов.
         * Примеры:
         *   --debug
         *   --debug-logs
         */
        private fun parseDebugLogs(args: Array<String>): Boolean {
            return args.any { it == "--debug" || it == "--debug-logs" }
        }

        /**
         * Парсит флаг AI-режима с опциональным портом.
         * Примеры:
         *   --ai-mode 8080   → порт 8080
         *   --ai-mode        → порт по умолчанию 8080
         *   --ai-mode=9999   → порт 9999
         *   (отсутствует)    → AI-режим выключен
         */
        private fun parseAiModePort(args: Array<String>): Int? {
            for (i in args.indices) {
                if (args[i] == "--ai-mode") {
                    if (i + 1 < args.size) {
                        val port = args[i + 1].toIntOrNull()
                        if (port != null) return port
                    }
                    return 8080
                }
                if (args[i].startsWith("--ai-mode=")) {
                    val value = args[i].substringAfter("=", "")
                    return value.toIntOrNull() ?: 8080
                }
            }
            return null
        }

        /**
         * Парсит id темы для отладки (форсирует тему всех карт, старт без меню).
         * Примеры:
         *   --theme castle
         *   --theme=castle
         *   (отсутствует) → тема из theme.json мира
         */
        private fun parseTheme(args: Array<String>): String? {
            for (i in args.indices) {
                if (args[i] == "--theme" && i + 1 < args.size) {
                    return args[i + 1]
                }
                if (args[i].startsWith("--theme=")) {
                    return args[i].substringAfter("=")
                }
            }
            return null
        }

        /** Dev-режим редактора карт (`--editor`). */
        private fun parseEditor(args: Array<String>): Boolean =
            args.any { it == "--editor" }

        /**
         * Путь к исходной папке assets для редактора.
         * Примеры: `--assets-dir C:\\...\\assets`, `--assets-dir=...`.
         */
        private fun parseAssetsDir(args: Array<String>): String? {
            for (i in args.indices) {
                if (args[i] == "--assets-dir" && i + 1 < args.size) {
                    return args[i + 1]
                }
                if (args[i].startsWith("--assets-dir=")) {
                    return args[i].substringAfter("=")
                }
            }
            return null
        }

    }
}
