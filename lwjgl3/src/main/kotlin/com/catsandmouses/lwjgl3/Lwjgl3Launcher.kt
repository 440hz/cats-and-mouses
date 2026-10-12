package com.catsandmouses.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter
import com.catsandmouses.MainGame
import com.catsandmouses.ShotScreen

class Lwjgl3Launcher {
    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val startMap = parseStartMap(args)
            val debugLogs = parseDebugLogs(args)
            val aiModePort = parseAiModePort(args)
            val themeId = parseTheme(args)
            val noCountdown = parseNoCountdown(args)
            val creditsMode = parseCredits(args)
            val editorMode = parseEditor(args)
            val assetsDir = parseAssetsDir(args)
            val shotScreen = ShotScreen.fromCode(parseShotScreen(args))
            val shotPath = parseShot(args)
            val window = parseWindow(args)
            val game = MainGame(
                startMap = startMap ?: "1/1",
                startMapExplicit = startMap != null,
                debugLogs = debugLogs,
                aiModePort = aiModePort,
                themeId = themeId,
                noCountdown = noCountdown,
                creditsMode = creditsMode,
                langOverride = parseLang(args),
                editorFactory = if (editorMode) { mg -> EditorScreen(mg, assetsDir) } else null,
                shotPath = shotPath,
                shotScreen = shotScreen,
                shotDelay = parseShotDelay(args)
            )
            Lwjgl3Application(game, Lwjgl3ApplicationConfiguration().apply {
                setTitle(if (editorMode) "CatsAndMouses — редактор карт" else "CatsAndMouses")
                setWindowedMode(window.first, window.second)
                // Закрытие окна перехватывается только редактором (подтверждение «Выходите!»)
                setWindowListener(object : Lwjgl3WindowAdapter() {
                    override fun closeRequested(): Boolean = EditorWindowClose.request()
                })
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

        /** Отключает стартовый отсчёт «3 → 2 → 1» (dev, `--no-countdown`). */
        private fun parseNoCountdown(args: Array<String>): Boolean =
            args.any { it == "--no-countdown" }

        /** Dev-режим редактора карт (`--editor`). */
        private fun parseEditor(args: Array<String>): Boolean =
            args.any { it == "--editor" }

        /** Dev: сразу открыть финальные титры (`--credits`). */
        private fun parseCredits(args: Array<String>): Boolean =
            args.any { it == "--credits" }

        /** Dev: форсировать язык интерфейса (`--lang ru`, `--lang en`, `--lang=en`). */
        private fun parseLang(args: Array<String>): String? {
            for (i in args.indices) {
                if (args[i] == "--lang" && i + 1 < args.size) {
                    return args[i + 1]
                }
                if (args[i].startsWith("--lang=")) {
                    return args[i].substringAfter("=")
                }
            }
            return null
        }

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

        /** Размер окна по умолчанию (dev). */
        private val DEFAULT_WINDOW = 1280 to 720

        /**
         * Размер окна (dev-флаг `--window 1920x1080`). По умолчанию 1280x720.
         */
        private fun parseWindow(args: Array<String>): Pair<Int, Int> {
            for (i in args.indices) {
                val value = when {
                    args[i] == "--window" && i + 1 < args.size -> args[i + 1]
                    args[i].startsWith("--window=") -> args[i].substringAfter("=")
                    else -> null
                } ?: continue
                val parts = value.lowercase().split("x")
                val w = parts.getOrNull(0)?.toIntOrNull()
                val h = parts.getOrNull(1)?.toIntOrNull()
                if (w != null && h != null && w > 0 && h > 0) return w to h
                return DEFAULT_WINDOW
            }
            return DEFAULT_WINDOW
        }

        /** Dev-флаг `--shot <path>`: сохранить кадр в PNG и выйти. */
        private fun parseShot(args: Array<String>): String? {
            for (i in args.indices) {
                if (args[i] == "--shot" && i + 1 < args.size) {
                    return args[i + 1]
                }
                if (args[i].startsWith("--shot=")) {
                    return args[i].substringAfter("=")
                }
            }
            return null
        }

        /** Dev-флаг `--shot-screen <id>`: экран для снимка. */
        private fun parseShotScreen(args: Array<String>): String? {
            for (i in args.indices) {
                if (args[i] == "--shot-screen" && i + 1 < args.size) {
                    return args[i + 1]
                }
                if (args[i].startsWith("--shot-screen=")) {
                    return args[i].substringAfter("=")
                }
            }
            return null
        }

        /** Dev-флаг `--shot-delay <сек>`: пауза перед снятием кадра. */
        private fun parseShotDelay(args: Array<String>): Float {
            for (i in args.indices) {
                val value = when {
                    args[i] == "--shot-delay" && i + 1 < args.size -> args[i + 1]
                    args[i].startsWith("--shot-delay=") -> args[i].substringAfter("=")
                    else -> null
                } ?: continue
                return value.toFloatOrNull() ?: 1f
            }
            return 1f
        }

    }
}
