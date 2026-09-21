package com.catsandmouses

import com.badlogic.gdx.Gdx

/**
 * Единственная точка переходов между экранами (MENU.md, §7).
 * Экраны не решают, «куда дальше» — это делает роутер.
 * Карты/уровни — из [LevelCatalog].
 */
class ScreenRouter(
    private val game: MainGame,
    private val progress: ProgressStore,
    private val catalog: LevelCatalog
) {

    /** В главное меню. */
    fun toMenu() {
        game.showMainMenu()
    }

    /** «Начать»: сброс прогресса и старт с первой карты. */
    fun startNewGame() {
        progress.clear()
        game.startGameDirect(catalog.mapName(0), 0)
    }

    /** «Продолжить»: старт с карты, где убили (жизни восстанавливаются из прогресса). */
    fun continueGame() {
        game.startGameDirect(
            catalog.mapName(progress.savedLevel()),
            progress.savedScore(),
            progress.savedLives()
        )
    }

    /** «Выход»: завершить приложение. */
    fun quit() {
        Gdx.app.exit()
    }

    /** Имя карты для индекса уровня (формат `debugStartMap`: `1/2`, `final`). */
    fun mapName(level: Int): String = catalog.mapName(level)
}
