package com.catsandmouses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ShotScreenTest {

    @Test
    fun `известные коды разбираются (регистр и пробелы не важны)`() {
        assertEquals(ShotScreen.LOADING, ShotScreen.fromCode("loading"))
        assertEquals(ShotScreen.MENU, ShotScreen.fromCode("MENU"))
        assertEquals(ShotScreen.SETTINGS, ShotScreen.fromCode(" settings "))
        assertEquals(ShotScreen.CREDITS, ShotScreen.fromCode("credits"))
        assertEquals(ShotScreen.GAME, ShotScreen.fromCode("game"))
    }

    @Test
    fun `пустое и неизвестное значение дают null`() {
        assertNull(ShotScreen.fromCode(null))
        assertNull(ShotScreen.fromCode(""))
        assertNull(ShotScreen.fromCode("pause"))
    }
}
