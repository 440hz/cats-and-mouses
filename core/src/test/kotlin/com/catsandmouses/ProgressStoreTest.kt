package com.catsandmouses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProgressStoreTest {

    private fun store() = FakePreferences().let { it to ProgressStore(it) }

    @Test
    fun `значения по умолчанию`() {
        val (_, store) = store()
        assertFalse(store.hasProgress())
        assertEquals(0, store.savedLevel())
        assertEquals(0, store.savedScore())
        assertEquals(0, store.bestScore())
        assertEquals(Constants.START_LIVES, store.savedLives())
    }

    @Test
    fun `сохранение при гибели`() {
        val (prefs, store) = store()
        store.saveOnDeath(level = 7, score = 1200)

        assertTrue(store.hasProgress())
        assertEquals(7, store.savedLevel())
        assertEquals(1200, store.savedScore())
        assertEquals(Constants.START_LIVES, store.savedLives(), "смерть сбрасывает жизни к стартовым")
        assertEquals(1, prefs.flushCount)
    }

    @Test
    fun `сохранение при переходе на следующий уровень`() {
        val (prefs, store) = store()
        store.saveOnAdvance(level = 3, score = 900, lives = 4)

        assertTrue(store.hasProgress())
        assertEquals(3, store.savedLevel())
        assertEquals(900, store.savedScore())
        assertEquals(4, store.savedLives(), "накопленные жизни переносятся")
        assertEquals(1, prefs.flushCount)
    }

    @Test
    fun `жизни при продвижении ограничены максимумом`() {
        val (_, store) = store()
        store.saveOnAdvance(level = 1, score = 0, lives = Constants.MAX_LIVES + 5)
        assertEquals(Constants.MAX_LIVES, store.savedLives())
    }

    @Test
    fun `лучший счёт обновляется только при росте`() {
        val (_, store) = store()
        store.saveOnDeath(0, 500)
        assertEquals(500, store.bestScore())

        store.saveOnAdvance(1, 300)
        assertEquals(500, store.bestScore(), "меньший счёт не должен понижать рекорд")

        store.saveOnAdvance(2, 500)
        assertEquals(500, store.bestScore(), "равный счёт не меняет рекорд")

        store.saveOnAdvance(3, 700)
        assertEquals(700, store.bestScore())
    }

    @Test
    fun `clear сбрасывает прогресс но сохраняет рекорд`() {
        val (prefs, store) = store()
        store.saveOnDeath(level = 5, score = 800)
        val flushAfterSave = prefs.flushCount

        store.clear()

        assertFalse(store.hasProgress())
        assertEquals(0, store.savedLevel())
        assertEquals(0, store.savedScore())
        assertEquals(Constants.START_LIVES, store.savedLives())
        assertEquals(800, store.bestScore(), "рекорд должен сохраниться после сброса")
        assertEquals(flushAfterSave + 1, prefs.flushCount)
    }

    @Test
    fun `последовательные смерти сохраняют последнее место гибели`() {
        val (_, store) = store()
        store.saveOnDeath(2, 100)
        store.saveOnDeath(4, 250)

        assertEquals(4, store.savedLevel())
        assertEquals(250, store.savedScore())
        assertEquals(250, store.bestScore())
    }

    @Test
    fun `clear на пустом прогрессе безопасен`() {
        val (_, store) = store()
        store.clear()
        assertFalse(store.hasProgress())
        assertEquals(0, store.savedLevel())
        assertEquals(0, store.bestScore())
    }

    @Test
    fun `громкость по умолчанию`() {
        val (_, store) = store()
        assertEquals(Constants.DEFAULT_MUSIC_VOLUME, store.musicVolume())
        assertEquals(Constants.DEFAULT_SFX_VOLUME, store.sfxVolume())
    }

    @Test
    fun `громкость сохраняется и ограничивается диапазоном`() {
        val (prefs, store) = store()
        store.setMusicVolume(30)
        store.setSfxVolume(70)
        assertEquals(30, store.musicVolume())
        assertEquals(70, store.sfxVolume())
        assertEquals(2, prefs.flushCount)

        store.setMusicVolume(150)
        store.setSfxVolume(-20)
        assertEquals(100, store.musicVolume(), "верхняя граница — 100")
        assertEquals(0, store.sfxVolume(), "нижняя граница — 0")
        assertEquals(100, prefs.getInteger(Constants.KEY_MUSIC_VOLUME, -1))
        assertEquals(0, prefs.getInteger(Constants.KEY_SFX_VOLUME, -1))
    }

    @Test
    fun `битые значения громкости в prefs ограничиваются при чтении`() {
        val (prefs, store) = store()
        prefs.putInteger(Constants.KEY_MUSIC_VOLUME, 999)
        prefs.putInteger(Constants.KEY_SFX_VOLUME, -5)
        assertEquals(100, store.musicVolume())
        assertEquals(0, store.sfxVolume())
    }

    @Test
    fun `язык по умолчанию не задан`() {
        val (_, store) = store()
        assertNull(store.savedLanguage(), "без выбора язык берётся из системной локали")
    }

    @Test
    fun `язык сохраняется и читается`() {
        val (prefs, store) = store()
        store.setLanguage("en")
        assertEquals("en", store.savedLanguage())
        assertEquals(1, prefs.flushCount)
        assertEquals("en", prefs.getString(Constants.KEY_LANGUAGE, null))
    }
}
