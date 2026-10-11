package com.catsandmouses

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.Screen
import com.catsandmouses.audio.AudioManager
import com.catsandmouses.i18n.I18n
import com.catsandmouses.i18n.Lang

class MainGame(
    private val startMap: String = "1/1",
    private val startMapExplicit: Boolean = false,
    private val debugLogs: Boolean = false,
    private val aiModePort: Int? = null,
    private val themeId: String? = null,
    /** Отключить стартовый отсчёт (dev-флаг `--no-countdown`). */
    private val noCountdown: Boolean = false,
    /** Dev-флаг `--credits`: сразу открыть финальные титры (пасхалку). */
    private val creditsMode: Boolean = false,
    /** Dev-флаг `--lang <ru|en>`: форсировать язык интерфейса. */
    private val langOverride: String? = null,
    /** Desktop-редактор карт (dev): создаёт экран без меню. null — обычная игра. */
    private val editorFactory: ((MainGame) -> Screen)? = null
) : Game() {

    private var aiDebugServer: AiDebugServer? = null
    lateinit var preferences: Preferences
        private set
    lateinit var progress: ProgressStore
        private set
    lateinit var audio: AudioManager
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
        // Язык интерфейса: форс флагом --lang, иначе сохранённый выбор или системная локаль (дефолт RU)
        I18n.lang = langOverride?.let { Lang.fromCode(it) }
            ?: progress.savedLanguage()?.let { Lang.fromCode(it) }
            ?: Lang.system()
        audio = AudioManager(progress)
        systemTheme = ThemeLoader.system()
        catalog = LevelCatalog()
        router = ScreenRouter(this, progress, catalog)
        Gdx.app.log(
            "MainGame",
            "create() startMap=$startMap explicit=$startMapExplicit debugLogs=$debugLogs " +
                "aiModePort=$aiModePort systemTheme=${systemTheme.activeTheme} " +
                "levels=${catalog.count} launchMode=${LaunchMode.suffix}"
        )

        // Dev: сразу финальные титры (пасхалка)
        if (creditsMode) {
            Gdx.app.log("MainGame", "credits mode")
            showCredits()
            return
        }

        // Dev-редактор карт (Desktop): отдельный экран, без меню и загрузки
        val factory = editorFactory
        if (factory != null) {
            Gdx.app.log("MainGame", "editor mode")
            setScreen(factory(this))
            return
        }

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
        setScreen(LoadingScreen(systemTheme, audio) { showMainMenu() })
    }

    /**
     * Показать главное меню (переходы — через ScreenRouter, MENU.md §3.1).
     */
    fun showMainMenu() {
        Gdx.app.log("MainGame", "showMainMenu()")
        val menu = MainMenuScreen(
            theme = systemTheme,
            preferences = preferences,
            audio = audio,
            onStartGame = { router.startNewGame() },
            onContinueGame = { router.continueGame() },
            onSettings = { router.openSettings() },
            onExit = { router.quit() }
        )
        setScreen(menu)
    }

    /**
     * Показать экран «Настройки» (MENU.md §3.7).
     */
    fun showSettings() {
        Gdx.app.log("MainGame", "showSettings()")
        setScreen(SettingsScreen(systemTheme, progress, audio) { showMainMenu() })
    }

    /**
     * Показать финальные титры (MENU.md §3.8). Кнопка «Начать сначала» —
     * сброс прогресса и новая игра с мира 1, карты 1.
     */
    fun showCredits() {
        Gdx.app.log("MainGame", "showCredits()")
        setScreen(
            CreditsScreen(
                theme = systemTheme,
                audio = audio,
                onStartOver = { router.startNewGame() },
                // Тап/клавиша во время прокрутки — в меню (пасхалка-карусель только для терпеливых)
                onSkipToMenu = { router.toMenu() }
            )
        )
    }

    /**
     * Запустить GameScreen с указанной картой.
     * Тема уровня берётся из каталога; при отладочном `--theme` — форсируется.
     */
    fun startGameDirect(map: String, startScore: Int = 0, startLives: Int = Constants.START_LIVES) {
        val index = catalog.indexOfMap(map)
        Gdx.app.log(
            "MainGame",
            "startGameDirect map=$map level=${index + 1}/${catalog.count} " +
                "theme=${themeId ?: catalog.themeFor(index)} startScore=$startScore startLives=$startLives"
        )
        val gameScreen = GameScreen(
            systemTheme = systemTheme,
            catalog = catalog,
            themeOverride = themeId,
            debugStartMap = map,
            debugLogs = debugLogs,
            initialScore = startScore,
            initialLives = startLives,
            // В AI-режиме и по dev-флагу отсчёт отключён (автотесты ждут PLAYING сразу)
            countdownEnabled = !noCountdown && aiModePort == null,
            mainGame = this
        )
        setScreen(gameScreen)

        aiModePort?.let {
            aiDebugServer = AiDebugServer(it, gameScreen).apply { start() }
        }
    }

    override fun pause() {
        // Сворачивание приложения (Android/Desktop): приглушаем музыку
        if (::audio.isInitialized) audio.pauseMusic()
        super.pause()
    }

    override fun resume() {
        if (::audio.isInitialized) audio.resumeMusic()
        super.resume()
    }

    override fun dispose() {
        aiDebugServer?.dispose()
        if (::audio.isInitialized) audio.dispose()
        super.dispose()
    }
}
