package com.catsandmouses.graphics

import com.catsandmouses.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CheeseAnimationTest {

    private val eps = 1e-3f

    @Test
    fun `набор содержит три акта`() {
        assertEquals(3, CheeseAnimation.acts.size)
        assertEquals(listOf(CheeseAnimation.wobble, CheeseAnimation.swell, CheeseAnimation.deflate), CheeseAnimation.acts)
    }

    @Test
    fun `длительности актов совпадают с константами`() {
        assertEquals(Constants.CHEESE_WOBBLE_DURATION, CheeseAnimation.wobble.duration)
        assertEquals(Constants.CHEESE_SWELL_DURATION, CheeseAnimation.swell.duration)
        assertEquals(Constants.CHEESE_DEFLATE_DURATION, CheeseAnimation.deflate.duration)
    }

    @Test
    fun `акты начинаются и заканчиваются единичным трансформом`() {
        CheeseAnimation.acts.forEach { act ->
            assertEquals(ActTransform.IDENTITY, act.transformAt(0f))
            assertEquals(ActTransform.IDENTITY, act.transformAt(1f))
        }
    }

    @Test
    fun `покачивание наклоняет на заданный угол в обе стороны`() {
        val angle = Constants.CHEESE_WOBBLE_ANGLE
        assertEquals(angle, CheeseAnimation.wobble.transformAt(0.25f).rotation, eps)
        assertEquals(-angle, CheeseAnimation.wobble.transformAt(0.75f).rotation, eps)
        assertEquals(0f, CheeseAnimation.wobble.transformAt(0.5f).rotation, eps)
    }

    @Test
    fun `разбухание растёт от стандарта до пика и обратно`() {
        assertEquals(Constants.CHEESE_SWELL_SCALE, CheeseAnimation.swell.transformAt(0.5f).scale, eps)
        assertTrue(Constants.CHEESE_SWELL_SCALE > 1f)
        // монотонно вверх до середины, затем вниз; никогда не ниже стандарта
        var prev = CheeseAnimation.swell.transformAt(0.01f).scale
        for (i in 2..50) {
            val s = CheeseAnimation.swell.transformAt(i / 100f).scale
            assertTrue(s >= prev - eps, "не растёт на шаге $i: $prev -> $s")
            prev = s
        }
        for (i in 51..99) {
            val s = CheeseAnimation.swell.transformAt(i / 100f).scale
            assertTrue(s <= prev + eps, "не спадает на шаге $i: $prev -> $s")
            prev = s
        }
        for (i in 0..100) {
            val s = CheeseAnimation.swell.transformAt(i / 100f).scale
            assertTrue(s >= 1f - eps, "ниже стандарта в разбухании: $s")
        }
    }

    @Test
    fun `сдувание уходит ниже стандарта и возвращается`() {
        assertEquals(Constants.CHEESE_DEFLATE_SCALE, CheeseAnimation.deflate.transformAt(0.5f).scale, eps)
        assertTrue(Constants.CHEESE_DEFLATE_SCALE < 1f)
        // монотонно вниз до середины, затем вверх; никогда не выше стандарта
        var prev = CheeseAnimation.deflate.transformAt(0.01f).scale
        for (i in 2..50) {
            val s = CheeseAnimation.deflate.transformAt(i / 100f).scale
            assertTrue(s <= prev + eps, "не сжимается на шаге $i: $prev -> $s")
            prev = s
        }
        for (i in 51..99) {
            val s = CheeseAnimation.deflate.transformAt(i / 100f).scale
            assertTrue(s >= prev - eps, "не возвращается на шаге $i: $prev -> $s")
            prev = s
        }
        for (i in 0..100) {
            val s = CheeseAnimation.deflate.transformAt(i / 100f).scale
            assertTrue(s <= 1f + eps, "выше стандарта в сдувании: $s")
        }
    }

    @Test
    fun `create повторяем по seed`() {
        val a = CheeseAnimation.create(CheeseAnimation.cellSeed(3, 7))
        val b = CheeseAnimation.create(CheeseAnimation.cellSeed(3, 7))
        repeat(500) {
            a.update(0.05f)
            b.update(0.05f)
            assertEquals(a.hasActiveAct(), b.hasActiveAct())
            assertEquals(a.transform(), b.transform())
        }
    }

    @Test
    fun `cellSeed стабилен и различает клетки`() {
        assertEquals(CheeseAnimation.cellSeed(3, 7), CheeseAnimation.cellSeed(3, 7))
        assertTrue(CheeseAnimation.cellSeed(3, 7) != CheeseAnimation.cellSeed(7, 3))
        assertTrue(CheeseAnimation.cellSeed(1, 1) != CheeseAnimation.cellSeed(1, 2))
    }

    @Test
    fun `за долгий прогон трансформ в допустимых границах`() {
        // несколько сыров в разных клетках — расписания разные, границы общие
        val cells = listOf(0 to 0, 3 to 5, 10 to 2, 20 to 12, 31 to 15)
        val animators = cells.map { (c, r) -> CheeseAnimation.create(CheeseAnimation.cellSeed(c, r)) }
        repeat(4000) {
            animators.forEach { a ->
                a.update(0.05f)
                val t = a.transform()
                assertTrue(t.scale in Constants.CHEESE_DEFLATE_SCALE - eps..Constants.CHEESE_SWELL_SCALE + eps)
                assertTrue(kotlin.math.abs(t.rotation) <= Constants.CHEESE_WOBBLE_ANGLE + eps)
                assertEquals(0f, t.offsetY)
            }
        }
    }
}
