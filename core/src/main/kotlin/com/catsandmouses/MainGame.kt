package com.catsandmouses

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

class MainGame(
    private val startMap: String = "1/1",
    private val debugLogs: Boolean = false,
    private val aiModePort: Int? = null
) : Game() {

    private var aiDebugServer: AiDebugServer? = null
    lateinit var preferences: Preferences
        private set

    override fun create() {
        preferences = Gdx.app.getPreferences(Constants.PREFS_NAME)
        Gdx.app.log("MainGame", "create() startMap=$startMap debugLogs=$debugLogs aiModePort=$aiModePort")

        // AI/дебуг-режим запускается сразу в GameScreen, минуя меню
        if (aiModePort != null || startMap != "1/1" || debugLogs) {
            Gdx.app.log("MainGame", "direct start")
            startGameDirect(startMap)
            return
        }

        showLoadingScreen()
    }

    /**
     * Показать экран загрузки, затем главное меню.
     */
    private fun showLoadingScreen() {
        setScreen(LoadingScreen { showMainMenu() })
    }

    /**
     * Показать главное меню.
     */
    fun showMainMenu() {
        Gdx.app.log("MainGame", "showMainMenu()")
        val menu = MainMenuScreen(
            preferences = preferences,
            onStartGame = { startNewGame() },
            onContinueGame = { continueGame() },
            onExit = { Gdx.app.exit() }
        )
        setScreen(menu)
    }

    /**
     * Начать новую игру с первой карты.
     */
    fun startNewGame() {
        preferences.putInteger(Constants.KEY_LAST_LEVEL, 0)
        preferences.putInteger(Constants.KEY_LAST_SCORE, 0)
        preferences.putBoolean(Constants.KEY_HAS_PROGRESS, false)
        preferences.flush()
        startGameDirect("1/1")
    }

    /**
     * Продолжить сохранённую игру.
     */
    private fun continueGame() {
        val level = preferences.getInteger(Constants.KEY_LAST_LEVEL, 0)
        val score = preferences.getInteger(Constants.KEY_LAST_SCORE, 0)
        val map = if (level in 0..3) "1/${level + 1}" else "final"
        startGameDirect(map, startScore = score)
    }

    /**
     * Запустить GameScreen с указанной картой.
     */
    private fun startGameDirect(map: String, startScore: Int = 0) {
        Gdx.app.log("MainGame", "startGameDirect map=$map startScore=$startScore")
        val gameScreen = GameScreen(
            debugStartMap = map,
            debugLogs = debugLogs,
            initialScore = startScore,
            mainGame = this
        )
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
