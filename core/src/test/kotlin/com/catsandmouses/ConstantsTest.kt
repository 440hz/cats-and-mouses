package com.catsandmouses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConstantsTest {

    @Test
    fun `геометрия лабиринта согласована`() {
        assertEquals(Constants.MAZE_COLS * Constants.CELL_SIZE, Constants.MAZE_WIDTH)
        assertEquals(Constants.MAZE_ROWS * Constants.CELL_SIZE, Constants.MAZE_HEIGHT)
    }

    @Test
    fun `лабиринт задан как 32 на 16 клеток`() {
        assertEquals(32, Constants.MAZE_COLS)
        assertEquals(16, Constants.MAZE_ROWS)
        assertEquals(40f, Constants.CELL_SIZE)
    }

    @Test
    fun `лабиринт центрируется по горизонтали и прижат к низу`() {
        assertEquals((Constants.WORLD_WIDTH - Constants.MAZE_WIDTH) / 2f, Constants.MAZE_OFFSET_X)
        assertEquals(0f, Constants.MAZE_OFFSET_Y)
    }

    @Test
    fun `ширина лабиринта не превышает мир`() {
        assertTrue(Constants.MAZE_WIDTH <= Constants.WORLD_WIDTH)
        assertTrue(Constants.MAZE_HEIGHT <= Constants.WORLD_HEIGHT)
    }

    @Test
    fun `версия имеет формат x y z`() {
        assertTrue(Regex("""\d+\.\d+\.\d+""").matches(Constants.VERSION), "VERSION=${Constants.VERSION}")
    }

    @Test
    fun `ключи Preferences уникальны и непусты`() {
        val keys = listOf(
            Constants.PREFS_NAME,
            Constants.KEY_BEST_SCORE,
            Constants.KEY_LAST_LEVEL,
            Constants.KEY_LAST_SCORE,
            Constants.KEY_HAS_PROGRESS,
            Constants.KEY_LIVES
        )
        assertTrue(keys.all { it.isNotBlank() })
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `здоровья и размеры интерфейса положительны`() {
        assertTrue(Constants.MAX_LIVES > 0)
        assertTrue(Constants.START_LIVES > 0)
        assertTrue(Constants.START_LIVES <= Constants.MAX_LIVES)
        assertTrue(Constants.HUD_HEART_SIZE > 0f)
        assertTrue(Constants.HUD_HEART_SIZE <= Constants.HEART_SIZE)
        assertTrue(Constants.INVULN_TIME > 0f)
        assertTrue(Constants.HUD_HEIGHT > 0f)
        assertTrue(Constants.BUTTON_WIDTH > 0f)
        assertTrue(Constants.BUTTON_HEIGHT > 0f)
        assertTrue(Constants.HEART_SIZE > 0f)
    }

    @Test
    fun `размеры шрифтов кратны 16`() {
        listOf(
            Constants.FONT_SIZE_HUD,
            Constants.FONT_SIZE_BUTTON,
            Constants.FONT_SIZE_OVERLAY,
            Constants.FONT_SIZE_TITLE,
            Constants.FONT_SIZE_COUNTDOWN
        ).forEach { assertTrue(it % 16 == 0, "размер шрифта $it не кратен 16") }
    }

    @Test
    fun `стартовый отсчёт длится три секунды`() {
        assertEquals(3f, Constants.COUNTDOWN_TIME)
        assertTrue(Constants.COUNTDOWN_TIME > 0f)
    }
}
