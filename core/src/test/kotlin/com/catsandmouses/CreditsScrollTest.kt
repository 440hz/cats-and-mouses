package com.catsandmouses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreditsScrollTest {

    @Test
    fun `на старте блок целиком под экраном и прокрутка не завершена`() {
        val scroll = CreditsScroll(totalHeight = 1000f, viewportHeight = 720f, speed = 100f)
        assertEquals(-1000f, scroll.offset)
        assertFalse(scroll.isFinished)
    }

    @Test
    fun `прокрутка поднимает блок со скоростью`() {
        val scroll = CreditsScroll(1000f, 720f, 100f)
        scroll.update(2f)
        assertEquals(-800f, scroll.offset)
    }

    @Test
    fun `прокрутка завершается когда блок уходит за верх и клампится`() {
        val scroll = CreditsScroll(1000f, 720f, 100f)
        scroll.update(20f)
        assertTrue(scroll.isFinished)
        assertEquals(720f, scroll.offset)
    }

    @Test
    fun `после завершения update не меняет offset`() {
        val scroll = CreditsScroll(100f, 720f, 100f)
        scroll.skip()
        scroll.update(5f)
        assertEquals(720f, scroll.offset)
    }

    @Test
    fun `skip показывает конец сразу`() {
        val scroll = CreditsScroll(1000f, 720f, 100f)
        scroll.skip()
        assertTrue(scroll.isFinished)
    }
}
