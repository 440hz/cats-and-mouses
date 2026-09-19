package com.catsandmouses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
    }

    @Test
    fun `сохранение при гибели`() {
        val (prefs, store) = store()
        store.saveOnDeath(level = 7, score = 1200)

        assertTrue(store.hasProgress())
        assertEquals(7, store.savedLevel())
        assertEquals(1200, store.savedScore())
        assertEquals(1, prefs.flushCount)
    }

    @Test
    fun `сохранение при переходе на следующий уровень`() {
        val (prefs, store) = store()
        store.saveOnAdvance(level = 3, score = 900)

        assertTrue(store.hasProgress())
        assertEquals(3, store.savedLevel())
        assertEquals(900, store.savedScore())
        assertEquals(1, prefs.flushCount)
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
}
