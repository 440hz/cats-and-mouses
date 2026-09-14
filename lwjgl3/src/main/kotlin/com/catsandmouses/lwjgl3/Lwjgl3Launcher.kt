package com.catsandmouses.lwjgl3

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter
import com.catsandmouses.MainGame
import java.awt.GraphicsEnvironment
import java.awt.Robot
import java.awt.event.InputEvent
import org.lwjgl.glfw.GLFW

class Lwjgl3Launcher {
    companion object {

        private var windowClosed = false

        @JvmStatic
        fun main(args: Array<String>) {
            val startMap = parseStartMap(args)
            val debugLogs = parseDebugLogs(args)
            val aiModePort = parseAiModePort(args)
            Lwjgl3Application(MainGame(startMap, debugLogs, aiModePort), Lwjgl3ApplicationConfiguration().apply {
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
         * Отладочный запуск с указанной карты.
         * Примеры:
         *   --start-map 1/2
         *   --start-map final
         *   --start-level 1/4  (обратная совместимость)
         */
        private fun parseStartMap(args: Array<String>): String {
            for (i in args.indices) {
                if ((args[i] == "--start-map" || args[i] == "--start-level") && i + 1 < args.size) {
                    return args[i + 1]
                }
                if (args[i].startsWith("--start-map=") || args[i].startsWith("--start-level=")) {
                    return args[i].substringAfter("=")
                }
            }
            return "1/1"
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
         * Удерживает фокус ввода на окне после старта приложения.
         * На Windows при запуске из Gradle/IDE новый процесс не может забрать
         * фокус сразу (foreground-лок), а консоль (особенно Windows Terminal)
         * часто перехватывает его обратно. Поэтому ~8 секунд следим за фокусом:
         * при потере просим его через GLFW, а если не помогло — активируем окно
         * синтетическим кликом (Robot), который Windows принимает как обычный
         * пользовательский ввод. Вызовы GLFW идут на render-потоке через
         * postRunnable.
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
                var clicksLeft = 2
                var lastFocused = true
                val deadline = System.currentTimeMillis() + 8000
                while (System.currentTimeMillis() < deadline && !windowClosed) {
                    try {
                        Thread.sleep(400)
                    } catch (_: InterruptedException) {
                        return@Thread
                    }
                    val focused = isWindowFocused(handle)
                    if (focused) {
                        if (!lastFocused) println("[Lwjgl3Launcher] focus: true")
                        lastFocused = true
                        continue
                    }
                    lastFocused = false
                    // Пробуем вернуть фокус штатными средствами GLFW
                    currentApp.postRunnable {
                        GLFW.glfwFocusWindow(handle)
                        GLFW.glfwRequestWindowAttention(handle)
                    }
                    Thread.sleep(150)
                    if (isWindowFocused(handle)) continue
                    // GLFW не смог (foreground-лок) — активируем окно кликом
                    if (clicksLeft > 0) {
                        clicksLeft--
                        currentApp.postRunnable { activateWindowByClick() }
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

        /**
         * Принудительная активация окна синтетическим кликом.
         * Окно при старте центрировано, поэтому кликаем в центр экрана
         * (координаты AWT, совместимые с Robot). Клик без смещения не
         * распознаётся как свайп и не влияет на игру.
         */
        private fun activateWindowByClick() {
            try {
                val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .defaultScreenDevice.defaultConfiguration.bounds
                val cx = bounds.x + bounds.width / 2
                val cy = bounds.y + bounds.height / 2
                val robot = Robot()
                robot.mouseMove(cx, cy)
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
            } catch (e: Exception) {
                Gdx.app?.log("Lwjgl3Launcher", "Не удалось активировать окно кликом: ${e.message}")
            }
        }
    }
}
