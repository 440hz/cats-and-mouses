package com.catsandmouses.lwjgl3

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter
import com.catsandmouses.MainGame
import org.lwjgl.glfw.GLFW

class Lwjgl3Launcher {
    companion object {

        private var windowClosed = false

        @JvmStatic
        fun main(args: Array<String>) {
            val startMap = parseStartMap(args)
            val debugLogs = parseDebugLogs(args)
            val aiModePort = parseAiModePort(args)
            val themeId = parseTheme(args)
            val game = MainGame(
                startMap = startMap ?: "1/1",
                startMapExplicit = startMap != null,
                debugLogs = debugLogs,
                aiModePort = aiModePort,
                themeId = themeId
            )
            Lwjgl3Application(game, Lwjgl3ApplicationConfiguration().apply {
                setTitle("CatsAndMouses")
                setWindowedMode(1280, 720)
                setWindowListener(object : Lwjgl3WindowAdapter() {
                    override fun created(window: Lwjgl3Window) {
                        val handle = window.windowHandle
                        // Окно поверх всех остальных (GLFW_FLOATING)
                        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE)
                        // Фокус ввода клавиатуры при показе окна
                        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_TRUE)
                        // Показать и передать фокус ввода клавиатуры сразу после создания
                        GLFW.glfwShowWindow(handle)
                        GLFW.glfwFocusWindow(handle)
                        GLFW.glfwRequestWindowAttention(handle)
                        // Windows при запуске из Gradle/IDE отдаёт фокус новому окну
                        // не сразу — повторно запрашиваем его чуть позже
                        requestFocusDelayed(handle)
                    }

                    override fun closeRequested(): Boolean {
                        windowClosed = true
                        return true
                    }
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

        /**
         * Удерживает фокус ввода на окне после старта приложения.
         * На Windows при запуске из Gradle/IDE новый процесс не может забрать
         * фокус сразу (foreground-лок), а консоль (особенно Windows Terminal)
         * часто перехватывает его обратно. Поэтому ~8 секунд следим за фокусом:
         * при потере просим его через GLFW (glfwFocusWindow). Вызовы GLFW идут
         * на render-потоке через postRunnable.
         *
         * Синтетический клик (Robot) НЕ используется: он выглядит как настоящий
         * и ломает интерактивные экраны, требующие явного ввода (экран загрузки
         * «нажми любую клавишу» и кнопки меню).
         */
        private fun requestFocusDelayed(handle: Long) {
            val worker = Thread {
                var app = Gdx.app
                var tries = 0
                while (app == null && tries < 20) {
                    try {
                        Thread.sleep(250)
                    } catch (_: InterruptedException) {
                        return@Thread
                    }
                    app = Gdx.app
                    tries++
                }
                val currentApp = app ?: return@Thread
                val deadline = System.currentTimeMillis() + 8000
                while (System.currentTimeMillis() < deadline && !windowClosed) {
                    try {
                        Thread.sleep(400)
                    } catch (_: InterruptedException) {
                        return@Thread
                    }
                    if (isWindowFocused(handle)) {
                        continue
                    }
                    // Фокус потерян — пробуем вернуть штатными средствами GLFW
                    currentApp.postRunnable {
                        GLFW.glfwFocusWindow(handle)
                        GLFW.glfwRequestWindowAttention(handle)
                    }
                }
            }
            worker.isDaemon = true
            worker.start()
        }

        /** Окно сейчас в фокусе ввода? */
        private fun isWindowFocused(handle: Long): Boolean {
            // Если окно уже закрыто, GLFW бросает исключение — ловим его.
            return try {
                GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE
            } catch (_: Exception) {
                false
            }
        }
    }
}
