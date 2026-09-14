package com.catsandmouses

import com.badlogic.gdx.Game

class MainGame(
    private val startMap: String = "1/1",
    private val debugLogs: Boolean = false,
    private val aiModePort: Int? = null
) : Game() {

    private var aiDebugServer: AiDebugServer? = null

    override fun create() {
        val gameScreen = GameScreen(startMap, debugLogs)
        setScreen(gameScreen)

        aiModePort?.let {
            aiDebugServer = AiDebugServer(it, gameScreen).apply { start() }
        }
    }

    override fun dispose() {
        aiDebugServer?.dispose()
        super.dispose()
    }
}
