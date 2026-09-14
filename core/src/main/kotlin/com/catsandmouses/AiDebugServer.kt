package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.JsonReader
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.BindException
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

/**
 * HTTP-сервер для AI-отладки.
 * Позволяет внешнему агенту читать состояние игры и управлять мышью.
 *
 * Эндпоинты:
 *   GET  /state — JSON с состоянием игры.
 *   POST /input — команда: move | pause | reset.
 */
class AiDebugServer(
    private val port: Int,
    private val gameScreen: GameScreen
) {
    private var server: HttpServer? = null

    /**
     * Запускает сервер. Должен вызываться после инициализации GameScreen,
     * чтобы состояние игры было доступно для чтения.
     */
    fun start() {
        try {
            server = HttpServer.create(InetSocketAddress(port), 0).apply {
                createContext("/state") { exchange -> handleState(exchange) }
                createContext("/input") { exchange -> handleInput(exchange) }
                executor = null // используем дефолтный однопоточный executor
                start()
            }
            Gdx.app.log("AI_DEBUG", "Server started on port $port")
        } catch (e: BindException) {
            Gdx.app.log("AI_DEBUG", "Port $port is busy, try another")
        } catch (e: Exception) {
            Gdx.app.error("AI_DEBUG", "Failed to start server on port $port", e)
        }
    }

    /**
     * Останавливает сервер. Вызывается при закрытии приложения.
     */
    fun dispose() {
        server?.stop(0)
        server = null
    }

    private fun handleState(exchange: HttpExchange) {
        if (exchange.requestMethod != "GET") {
            sendJson(exchange, 405, """{"error":"Method not allowed"}""")
            return
        }
        sendJson(exchange, 200, buildStateJson())
    }

    private fun buildStateJson(): String {
        val cats = gameScreen.getCatsInfo()
        val cheese = gameScreen.getRemainingCheese()

        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"game_state\": \"${gameScreen.getGameState()}\",")
        sb.appendLine("  \"score\": ${gameScreen.getScore()},")
        sb.appendLine("  \"map\": \"${gameScreen.getCurrentMapName()}\",")

        val mouse = gameScreen.getMouseCell()
        sb.appendLine("  \"mouse\": {\"x\": ${mouse.first}, \"y\": ${mouse.second}, \"direction\": \"${gameScreen.getMouseDirection()}\"},")

        sb.appendLine("  \"cats\": [")
        cats.forEachIndexed { index, cat ->
            sb.append("    {\"name\": \"${cat["name"]}\", \"type\": \"${cat["type"]}\", \"x\": ${cat["x"]}, \"y\": ${cat["y"]}}")
            if (index < cats.size - 1) sb.append(",")
            sb.appendLine()
        }
        sb.appendLine("  ],")

        sb.appendLine("  \"cheese\": [")
        cheese.forEachIndexed { index, c ->
            sb.append("    {\"x\": ${c.first}, \"y\": ${c.second}}")
            if (index < cheese.size - 1) sb.append(",")
            sb.appendLine()
        }
        sb.appendLine("  ],")

        sb.appendLine("  \"cheese_collected\": ${gameScreen.getCollectedCheeseCount()},")
        sb.appendLine("  \"cheese_total\": ${gameScreen.getTotalCheeseCount()},")
        sb.appendLine("  \"lives\": ${gameScreen.getLives()}")
        sb.append("}")
        return sb.toString()
    }

    private fun handleInput(exchange: HttpExchange) {
        if (exchange.requestMethod != "POST") {
            sendJson(exchange, 405, """{"error":"Method not allowed"}""")
            return
        }

        val body = exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8)
        Gdx.app.log("AI_DEBUG", "Command: $body")

        try {
            val json = JsonReader().parse(body)
            val action = json.getString("action", "")

            // Команды, изменяющие состояние, выполняем в главном потоке LibGDX,
            // чтобы избежать гонок с игровым циклом.
            when (action) {
                "move" -> {
                    val dirString = json.getString("direction", "NONE")
                    val direction = Direction.valueOf(dirString)
                    Gdx.app.postRunnable { gameScreen.setAiDirection(direction) }
                }
                "pause" -> {
                    Gdx.app.postRunnable { gameScreen.toggleAiPause() }
                }
                "reset" -> {
                    Gdx.app.postRunnable { gameScreen.resetLevel() }
                }
                else -> {
                    sendJson(exchange, 400, """{"error":"Unknown action: $action"}""")
                    return
                }
            }

            sendJson(exchange, 200, """{"status":"ok"}""")
        } catch (e: IllegalArgumentException) {
            Gdx.app.error("AI_DEBUG", "Invalid direction in command", e)
            sendJson(exchange, 400, """{"error":"Invalid direction"}""")
        } catch (e: Exception) {
            Gdx.app.error("AI_DEBUG", "Failed to parse command", e)
            sendJson(exchange, 400, """{"error":"Invalid JSON"}""")
        }
    }

    private fun sendJson(exchange: HttpExchange, code: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
