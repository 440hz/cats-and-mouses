package com.catsandmouses.graphics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AnimatedSpriteTest {

    private val frames = 6
    private val duration = 0.1f

    @Test
    fun `в начале времени показывается первый кадр`() {
        assertEquals(0, AnimatedSprite.frameIndexAt(0f, 0f, frames, duration))
    }

    @Test
    fun `индекс растёт вместе со временем`() {
        assertEquals(0, AnimatedSprite.frameIndexAt(0.05f, 0f, frames, duration))
        assertEquals(1, AnimatedSprite.frameIndexAt(0.15f, 0f, frames, duration))
        assertEquals(5, AnimatedSprite.frameIndexAt(0.55f, 0f, frames, duration))
    }

    @Test
    fun `цикл замыкается на границе`() {
        assertEquals(0, AnimatedSprite.frameIndexAt(0.6f, 0f, frames, duration))
        assertEquals(2, AnimatedSprite.frameIndexAt(0.6f + 0.25f, 0f, frames, duration))
    }

    @Test
    fun `фаза сдвигает кадр`() {
        assertEquals(3, AnimatedSprite.frameIndexAt(0f, 3 * duration, frames, duration))
        assertEquals(0, AnimatedSprite.frameIndexAt(3 * duration, 3 * duration, frames, duration))
    }

    @Test
    fun `отрицательное время нормализуется в диапазон`() {
        val index = AnimatedSprite.frameIndexAt(-0.1f, 0f, frames, duration)
        assertEquals(5, index)
    }

    @Test
    fun `некорректные параметры отклоняются`() {
        assertThrows(IllegalArgumentException::class.java) {
            AnimatedSprite.frameIndexAt(0f, 0f, 0, duration)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AnimatedSprite.frameIndexAt(0f, 0f, frames, 0f)
        }
    }
}
