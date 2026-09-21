package com.catsandmouses.graphics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContinuousAnimatorTest {

    private val eps = 1e-3f

    /** Простой эффект: масштаб линейно от прогресса. */
    private val linear: (Float) -> ActTransform = { t -> ActTransform(scale = 1f + t) }

    private fun animator(period: Float = 1f, phase: Float = 0f) =
        ContinuousAnimator(period, phase, linear)

    @Test
    fun `на старте прогресс равен фазе`() {
        assertEquals(1f, animator(phase = 0f).transform().scale, eps)
        assertEquals(1.25f, animator(phase = 0.25f).transform().scale, eps)
    }

    @Test
    fun `время двигает прогресс`() {
        val a = animator(period = 1f)
        a.update(0.5f)
        assertEquals(1.5f, a.transform().scale, eps)
    }

    @Test
    fun `фаза складывается со временем`() {
        val a = animator(period = 1f, phase = 0.25f)
        a.update(0.25f)
        assertEquals(1.5f, a.transform().scale, eps)
    }

    @Test
    fun `цикл повторяется ровно через период`() {
        val a = animator(period = 1f)
        a.update(0.3f)
        val before = a.transform().scale
        a.update(1f)
        assertEquals(before, a.transform().scale, eps)
    }

    @Test
    fun `прогресс всегда в границах`() {
        val a = animator(period = 1f, phase = 0.7f)
        repeat(500) {
            a.update(0.13f)
            val s = a.transform().scale
            assertTrue(s >= 1f - eps && s <= 2f + eps, "вне диапазона: $s")
        }
    }

    @Test
    fun `нулевая и отрицательная дельта не двигают время`() {
        val a = animator(period = 1f, phase = 0f)
        a.update(0.4f)
        a.update(0f)
        a.update(-1f)
        assertEquals(1.4f, a.transform().scale, eps)
    }

    @Test
    fun `детерминирован по параметрам`() {
        val a = animator(period = 0.9f, phase = 0.3f)
        val b = animator(period = 0.9f, phase = 0.3f)
        repeat(100) {
            a.update(0.07f)
            b.update(0.07f)
            assertEquals(a.transform(), b.transform())
        }
    }

    @Test
    fun `период должен быть положительным`() {
        assertThrows(IllegalArgumentException::class.java) { animator(period = 0f) }
    }
}
