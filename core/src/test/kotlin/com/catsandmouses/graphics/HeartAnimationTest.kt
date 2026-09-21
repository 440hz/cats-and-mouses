package com.catsandmouses.graphics

import com.catsandmouses.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HeartAnimationTest {

    private val eps = 1e-3f

    @Test
    fun `на старте цикла масштаб стандартный`() {
        assertEquals(1f, HeartAnimation.transformAt(0f).scale, eps)
    }

    @Test
    fun `в середине цикла пиковый масштаб`() {
        assertEquals(Constants.HEART_PULSE_SCALE, HeartAnimation.transformAt(0.5f).scale, eps)
        assertTrue(Constants.HEART_PULSE_SCALE > 1f)
    }

    @Test
    fun `на конце цикла снова стандартный`() {
        assertEquals(1f, HeartAnimation.transformAt(1f).scale, eps)
    }

    @Test
    fun `масштаб всегда в диапазоне от стандарта до пика`() {
        for (i in 0..100) {
            val s = HeartAnimation.transformAt(i / 100f).scale
            assertTrue(s in 1f - eps..Constants.HEART_PULSE_SCALE + eps, "вне диапазона: $s")
        }
    }

    @Test
    fun `до середины масштаб растёт, затем спадает`() {
        var prev = HeartAnimation.transformAt(0f).scale
        for (i in 1..50) {
            val s = HeartAnimation.transformAt(i / 100f).scale
            assertTrue(s >= prev - eps, "не растёт на шаге $i: $prev -> $s")
            prev = s
        }
        for (i in 51..100) {
            val s = HeartAnimation.transformAt(i / 100f).scale
            assertTrue(s <= prev + eps, "не спадает на шаге $i: $prev -> $s")
            prev = s
        }
    }

    @Test
    fun `трансформ не содержит поворота и смещения`() {
        val t = HeartAnimation.transformAt(0.5f)
        assertEquals(0f, t.rotation)
        assertEquals(0f, t.offsetY)
    }

    @Test
    fun `create повторяем по seed`() {
        val a = HeartAnimation.create(HeartAnimation.cellSeed(3, 7))
        val b = HeartAnimation.create(HeartAnimation.cellSeed(3, 7))
        repeat(300) {
            a.update(0.05f)
            b.update(0.05f)
            assertEquals(a.transform(), b.transform())
        }
    }

    @Test
    fun `create держит масштаб в допустимых границах`() {
        val cells = listOf(0 to 0, 3 to 5, 10 to 2, 20 to 12, 31 to 15)
        val animators = cells.map { (c, r) -> HeartAnimation.create(HeartAnimation.cellSeed(c, r)) }
        repeat(4000) {
            animators.forEach { a ->
                a.update(0.03f)
                assertTrue(a.transform().scale in 1f - eps..Constants.HEART_PULSE_SCALE + eps)
            }
        }
    }

    @Test
    fun `cellSeed стабилен и различает клетки`() {
        assertEquals(HeartAnimation.cellSeed(3, 7), HeartAnimation.cellSeed(3, 7))
        assertTrue(HeartAnimation.cellSeed(3, 7) != HeartAnimation.cellSeed(7, 3))
        assertTrue(HeartAnimation.cellSeed(1, 1) != HeartAnimation.cellSeed(1, 2))
    }
}
