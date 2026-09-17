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
    lateinit var progress: ProgressStore
        private set
    lateinit var router: ScreenRouter
        private set

    override fun create() {
        preferences = Gdx.app.getPreferences(Constants.PREFS_NAME)
        progress = ProgressStore(preferences)
        router = ScreenRouter(this, progress)
        Gdx.app.log("MainGame", "create() startMap=$startMap debugLogs=$debugLogs aiModePort=$aiModePort launchMode=${LaunchMode.suffix}")

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
     * Показать главное меню (переходы — через ScreenRouter, MENU.md §3.1).
     */
    fun showMainMenu() {
        Gdx.app.log("MainGame", "showMainMenu()")
        val menu = MainMenuScreen(
            preferences = preferences,
            onStartGame = { router.startNewGame() },
            onContinueGame = { router.continueGame() },
            onExit = { router.quit() }
        )
        setScreen(menu)
    }

    /**
     * Запустить GameScreen с указанной картой.
     */
    fun startGameDirect(map: String, startScore: Int = 0) {
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
