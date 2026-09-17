package com.catsandmouses

import com.badlogic.gdx.Gdx

/**
 * Единственная точка переходов между экранами (MENU.md, §7).
 * Экраны не решают, «куда дальше» — это делает роутер.
 */
class ScreenRouter(
    private val game: MainGame,
    private val progress: ProgressStore
) {

    /** В главное меню. */
    fun toMenu() {
        game.showMainMenu()
    }

    /** «Начать»: сброс прогресса и старт с карты 1. */
    fun startNewGame() {
        progress.clear()
        game.startGameDirect(mapName(0), 0)
    }

    /** «Продолжить»: старт с карты, где убили. */
    fun continueGame() {
        game.startGameDirect(mapName(progress.savedLevel()), progress.savedScore())
    }

    /** «Выход»: завершить приложение. */
    fun quit() {
        Gdx.app.exit()
    }

    /** Имя карты для индекса уровня (формат `debugStartMap`: `1/2`, `final`). */
    fun mapName(level: Int): String {
        val index = level.coerceIn(0, Constants.MAP_FILES.size - 1)
        return Constants.MAP_FILES[index].removePrefix("maps/").removeSuffix(".txt")
    }
}
