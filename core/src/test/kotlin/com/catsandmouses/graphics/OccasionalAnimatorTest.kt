package com.catsandmouses.graphics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OccasionalAnimatorTest {

    private val fixedAct = Act(1f) { t -> ActTransform(scale = 1f + t) }

    private fun animator(
        acts: List<Act> = listOf(fixedAct),
        min: Float = 2f,
        max: Float = 2f,
        seed: Long = 42L
    ) = OccasionalAnimator(acts, min, max, seed)

    @Test
    fun `в покое трансформ единичный`() {
        val a = animator()
        assertTrue(a.transform().isIdentity)
        a.update(1.9f)
        assertFalse(a.hasActiveAct())
        assertTrue(a.transform().isIdentity)
    }

    @Test
    fun `акт начинается по истечении интервала`() {
        val a = animator()
        a.update(2f)
        assertTrue(a.hasActiveAct())
    }

    @Test
    fun `во время акта эффект применяется не сразу и растёт`() {
        val a = animator()
        a.update(2f)
        assertTrue(a.transform().isIdentity, "на старте акта (t=0) эффект единичный")
        a.update(0.5f)
        assertEquals(1.5f, a.transform().scale, 1e-4f)
        a.update(0.25f)
        assertEquals(1.75f, a.transform().scale, 1e-4f)
    }

    @Test
    fun `после акта возвращается ровно в identity`() {
        val a = animator()
        a.update(2f)
        a.update(1f)
        assertFalse(a.hasActiveAct())
        assertTrue(a.transform().isIdentity)
        assertEquals(1f, a.transform().scale)
        assertEquals(0f, a.transform().rotation)
    }

    @Test
    fun `акты не накладываются друг на друга`() {
        val a = animator()
        a.update(2f)
        // внутри акта интервал не должен запускать новый акт
        a.update(0.9f)
        assertTrue(a.hasActiveAct())
        a.update(0.05f)
        assertTrue(a.hasActiveAct())
        a.update(0.1f)
        assertFalse(a.hasActiveAct())
    }

    @Test
    fun `большой delta не ломает логику`() {
        val a = animator()
        a.update(1000f)
        val t = a.transform()
        assertTrue(t.scale.isFinite() && t.rotation.isFinite() && t.offsetY.isFinite())
    }

    @Test
    fun `нулевой и отрицательный delta безопасны`() {
        val a = animator()
        a.update(1f)
        val before = a.transform()
        a.update(0f)
        a.update(-5f)
        assertEquals(before, a.transform())
    }

    @Test
    fun `reset возвращает в покой`() {
        val a = animator()
        a.update(2.5f)
        assertTrue(a.hasActiveAct())
        a.reset()
        assertFalse(a.hasActiveAct())
        assertTrue(a.transform().isIdentity)
    }

    @Test
    fun `один seed даёт одинаковое расписание`() {
        val x = animator(min = 0.5f, max = 1.5f, seed = 7L)
        val y = animator(min = 0.5f, max = 1.5f, seed = 7L)
        repeat(400) {
            x.update(0.05f)
            y.update(0.05f)
            assertEquals(x.hasActiveAct(), y.hasActiveAct())
            assertEquals(x.transform(), y.transform())
        }
    }

    @Test
    fun `разные seed дают разные расписания`() {
        val seeds = listOf(1L, 2L, 3L, 4L, 5L)
        val signatures = seeds.map { seed ->
            val a = animator(min = 0.5f, max = 1.5f, seed = seed)
            buildString {
                repeat(400) {
                    a.update(0.05f)
                    append(if (a.hasActiveAct()) '1' else '0')
                }
            }
        }
        assertTrue(signatures.toSet().size > 1, "расписания не должны совпадать у всех seed")
    }

    @Test
    fun `интервал всегда в заданных границах`() {
        val a = animator(min = 1f, max = 3f, seed = 99L)
        // акт длится 1 с; если бы интервал был вне границ, моменты старта «поехали» бы
        var prevActive = false
        var sinceEnd = 0f
        var starts = 0
        repeat(2000) {
            a.update(0.05f)
            val active = a.hasActiveAct()
            if (active && !prevActive) {
                if (starts > 0) assertTrue(sinceEnd >= 1f - 1e-3f)
                starts++
                sinceEnd = 0f
            }
            if (!active) sinceEnd += 0.05f
            prevActive = active
        }
        assertTrue(starts > 3, "должно произойти несколько актов")
    }

    @Test
    fun `один и тот же акт не повторяется подряд`() {
        // Разные акты дают разные константные масштабы — по ним и распознаём акт.
        val acts = listOf(
            Act(1f) { ActTransform(scale = 2f) },
            Act(1f) { ActTransform(scale = 3f) },
            Act(1f) { ActTransform(scale = 4f) }
        )
        val a = OccasionalAnimator(acts, 0.5f, 0.5f, seed = 5L)
        val sequence = mutableListOf<Float>()
        repeat(20000) {
            a.update(0.05f)
            val t = a.transform()
            if (a.hasActiveAct() && t.scale != 1f && (sequence.isEmpty() || sequence.last() != t.scale)) {
                sequence.add(t.scale)
            }
        }
        assertTrue(sequence.size > 10, "должно быть много актов, а их ${sequence.size}")
        for (i in 1 until sequence.size) {
            assertTrue(sequence[i] != sequence[i - 1], "акт повторился подряд: ${sequence[i]}")
        }
    }

    @Test
    fun `некорректные параметры отклоняются`() {
        assertThrows(IllegalArgumentException::class.java) {
            OccasionalAnimator(emptyList(), 1f, 2f, 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OccasionalAnimator(listOf(fixedAct), 0f, 2f, 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OccasionalAnimator(listOf(fixedAct), 3f, 1f, 0L)
        }
        assertThrows(IllegalArgumentException::class.java) { Act(0f) { ActTransform() } }
    }
}
