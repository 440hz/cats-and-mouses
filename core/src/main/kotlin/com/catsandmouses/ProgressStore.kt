package com.catsandmouses

import com.badlogic.gdx.Preferences

/**
 * Единственная точка работы с сохранённым прогрессом (Preferences).
 * Спецификация — MENU.md, §6.
 */
class ProgressStore(private val prefs: Preferences) {

    fun hasProgress(): Boolean = prefs.getBoolean(Constants.KEY_HAS_PROGRESS, false)

    fun savedLevel(): Int = prefs.getInteger(Constants.KEY_LAST_LEVEL, 0)

    fun savedScore(): Int = prefs.getInteger(Constants.KEY_LAST_SCORE, 0)

    fun bestScore(): Int = prefs.getInteger(Constants.KEY_BEST_SCORE, 0)

    /** Сохранить уровень гибели и счёт (для «Продолжить»). */
    fun saveOnDeath(level: Int, score: Int) {
        rememberBest(score)
        prefs.putInteger(Constants.KEY_LAST_LEVEL, level)
        prefs.putInteger(Constants.KEY_LAST_SCORE, score)
        prefs.putBoolean(Constants.KEY_HAS_PROGRESS, true)
        prefs.flush()
    }

    /** Сохранить продвижение на следующий уровень. */
    fun saveOnAdvance(level: Int, score: Int) {
        rememberBest(score)
        prefs.putInteger(Constants.KEY_LAST_LEVEL, level)
        prefs.putInteger(Constants.KEY_LAST_SCORE, score)
        prefs.putBoolean(Constants.KEY_HAS_PROGRESS, true)
        prefs.flush()
    }

    /** Сбросить прогресс (новая игра / прохождение финала). */
    fun clear() {
        prefs.putInteger(Constants.KEY_LAST_LEVEL, 0)
        prefs.putInteger(Constants.KEY_LAST_SCORE, 0)
        prefs.putBoolean(Constants.KEY_HAS_PROGRESS, false)
        prefs.flush()
    }

    private fun rememberBest(score: Int) {
        if (score > bestScore()) {
            prefs.putInteger(Constants.KEY_BEST_SCORE, score)
        }
    }
}
