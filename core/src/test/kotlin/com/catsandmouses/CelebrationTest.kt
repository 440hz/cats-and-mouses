package com.catsandmouses

import com.catsandmouses.graphics.Celebration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CelebrationTest {

    @Test
    fun `на старте праздник не завершён и пуст`() {
        val c = Celebration(duration = 5f, seed = 1L)
        assertFalse(c.isFinished)
        assertEquals(0f, c.progress)
        assertTrue(c.balloons.isEmpty())
        assertTrue(c.sparks.isEmpty())
    }

    @Test
    fun `после суммарного времени не меньше длительности праздник завершён`() {
        val c = Celebration(duration = 5f, seed = 1L)
        repeat(6) { c.update(1f) }
        assertTrue(c.isFinished)
        assertEquals(1f, c.progress)
    }

    @Test
    fun `шарики запускаются и поднимаются вверх`() {
        val c = Celebration(duration = 5f, seed = 7L)
        c.update(0.3f)
        assertTrue(c.balloons.isNotEmpty())
        val y0 = c.balloons.first().y
        c.update(0.3f)
        assertTrue(c.balloons.first().y > y0)
    }

    @Test
    fun `салют порождает искры`() {
        val c = Celebration(duration = 5f, seed = 3L)
        c.update(0.7f)
        assertTrue(c.sparks.isNotEmpty())
    }

    @Test
    fun `искры гаснут и удаляются по истечении жизни`() {
        val c = Celebration(duration = 30f, seed = 5L)
        c.update(0.7f)
        val had = c.sparks.isNotEmpty()
        repeat(200) { c.update(0.1f) } // много времени назад: старые искры удаляются
        assertTrue(had)
        // Старые искры удалены (остаются только свежие вспышки этого кадра — их число ограничено)
        assertTrue(c.sparks.size < 400)
    }

    @Test
    fun `два праздника с одним seed воспроизводимы`() {
        val a = Celebration(duration = 5f, seed = 42L)
        val b = Celebration(duration = 5f, seed = 42L)
        repeat(5) { a.update(0.25f); b.update(0.25f) }
        assertEquals(a.balloons.size, b.balloons.size)
        assertEquals(a.sparks.size, b.sparks.size)
        assertEquals(a.balloons.first().x, b.balloons.first().x)
    }
}
