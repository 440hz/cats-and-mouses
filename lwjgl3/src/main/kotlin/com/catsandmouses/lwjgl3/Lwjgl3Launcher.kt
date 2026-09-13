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
            Lwjgl3Application(MainGame(startMap, debugLogs), Lwjgl3ApplicationConfiguration().apply {
                setTitle("CatsAndMouses")
                setWindowedMode(1280, 720)
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
    }
}
