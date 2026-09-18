package com.catsandmouses

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

class MainGame(
    private val startMap: String = "1/1",
    private val startMapExplicit: Boolean = false,
    private val debugLogs: Boolean = false,
    private val aiModePort: Int? = null,
    private val themeId: String? = null
) : Game() {

    private var aiDebugServer: AiDebugServer? = null
    lateinit var preferences: Preferences
        private set
    lateinit var progress: ProgressStore
        private set
    lateinit var router: ScreenRouter
        private set

    /** Системная тема: заставка, главное меню и оверлеи. */
    lateinit var systemTheme: ThemeLoader
        private set

    /** Каталог уровней (миры → карты → final). */
    lateinit var catalog: LevelCatalog
        private set

    override fun create() {
        preferences = Gdx.app.getPreferences(Constants.PREFS_NAME)
        progress = ProgressStore(preferences)
        systemTheme = ThemeLoader.system()
        catalog = LevelCatalog()
        router = ScreenRouter(this, progress, catalog)
        Gdx.app.log(
            "MainGame",
            "create() startMap=$startMap explicit=$startMapExplicit debugLogs=$debugLogs " +
                "aiModePort=$aiModePort systemTheme=${systemTheme.activeTheme} " +
                "levels=${catalog.count} launchMode=${LaunchMode.suffix}"
        )

        // Отладка: --ai-mode / --start-map / --debug / --theme — сразу в игру, без меню
        if (aiModePort != null || startMapExplicit || debugLogs || themeId != null) {
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
        setScreen(LoadingScreen(systemTheme) { showMainMenu() })
    }

    /**
     * Показать главное меню (переходы — через ScreenRouter, MENU.md §3.1).
     */
    fun showMainMenu() {
        Gdx.app.log("MainGame", "showMainMenu()")
        val menu = MainMenuScreen(
            theme = systemTheme,
            preferences = preferences,
            onStartGame = { router.startNewGame() },
            onContinueGame = { router.continueGame() },
            onExit = { router.quit() }
        )
        setScreen(menu)
    }

    /**
     * Запустить GameScreen с указанной картой.
     * Тема уровня берётся из каталога; при отладочном `--theme` — форсируется.
     */
    fun startGameDirect(map: String, startScore: Int = 0) {
        val index = catalog.indexOfMap(map)
        Gdx.app.log(
            "MainGame",
            "startGameDirect map=$map level=${index + 1}/${catalog.count} " +
                "theme=${themeId ?: catalog.themeFor(index)} startScore=$startScore"
        )
        val gameScreen = GameScreen(
            systemTheme = systemTheme,
            catalog = catalog,
            themeOverride = themeId,
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
