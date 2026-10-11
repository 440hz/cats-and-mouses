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

    /** Сохранённый запас жизней (для «Продолжить»). */
    fun savedLives(): Int = prefs.getInteger(Constants.KEY_LIVES, Constants.START_LIVES)

    fun bestScore(): Int = prefs.getInteger(Constants.KEY_BEST_SCORE, 0)

    /** Громкость музыки, 0…100 (дефолт [Constants.DEFAULT_MUSIC_VOLUME]). */
    fun musicVolume(): Int =
        prefs.getInteger(Constants.KEY_MUSIC_VOLUME, Constants.DEFAULT_MUSIC_VOLUME).coerceIn(0, 100)

    /** Громкость звуков (SFX), 0…100 (дефолт [Constants.DEFAULT_SFX_VOLUME]). */
    fun sfxVolume(): Int =
        prefs.getInteger(Constants.KEY_SFX_VOLUME, Constants.DEFAULT_SFX_VOLUME).coerceIn(0, 100)

    fun setMusicVolume(value: Int) {
        prefs.putInteger(Constants.KEY_MUSIC_VOLUME, value.coerceIn(0, 100))
        prefs.flush()
    }

    fun setSfxVolume(value: Int) {
        prefs.putInteger(Constants.KEY_SFX_VOLUME, value.coerceIn(0, 100))
        prefs.flush()
    }

    /**
     * Код сохранённого языка интерфейса (`ru`/`en`) или `null`, если пользователь
     * ещё не выбирал (тогда язык берётся из системной локали).
     */
    fun savedLanguage(): String? = prefs.getString(Constants.KEY_LANGUAGE, null)

    /** Сохранить выбранный язык интерфейса (`ru`/`en`). */
    fun setLanguage(code: String) {
        prefs.putString(Constants.KEY_LANGUAGE, code)
        prefs.flush()
    }

    /**
     * Сохранить уровень гибели и счёт (для «Продолжить»). Запас жизней на смерть
     * обнулён, поэтому новая попытка начинается с [Constants.START_LIVES].
     */
    fun saveOnDeath(level: Int, score: Int) {
        rememberBest(score)
        prefs.putInteger(Constants.KEY_LAST_LEVEL, level)
        prefs.putInteger(Constants.KEY_LAST_SCORE, score)
        prefs.putInteger(Constants.KEY_LIVES, Constants.START_LIVES)
        prefs.putBoolean(Constants.KEY_HAS_PROGRESS, true)
        prefs.flush()
    }

    /** Сохранить продвижение на следующий уровень вместе с накопленными жизнями. */
    fun saveOnAdvance(level: Int, score: Int, lives: Int = Constants.START_LIVES) {
        rememberBest(score)
        prefs.putInteger(Constants.KEY_LAST_LEVEL, level)
        prefs.putInteger(Constants.KEY_LAST_SCORE, score)
        prefs.putInteger(Constants.KEY_LIVES, lives.coerceIn(Constants.START_LIVES, Constants.MAX_LIVES))
        prefs.putBoolean(Constants.KEY_HAS_PROGRESS, true)
        prefs.flush()
    }

    /** Сбросить прогресс (новая игра / прохождение финала). */
    fun clear() {
        prefs.putInteger(Constants.KEY_LAST_LEVEL, 0)
        prefs.putInteger(Constants.KEY_LAST_SCORE, 0)
        prefs.putInteger(Constants.KEY_LIVES, Constants.START_LIVES)
        prefs.putBoolean(Constants.KEY_HAS_PROGRESS, false)
        prefs.flush()
    }

    private fun rememberBest(score: Int) {
        if (score > bestScore()) {
            prefs.putInteger(Constants.KEY_BEST_SCORE, score)
        }
    }
}
