package com.catsandmouses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class DirectionTest {

    @Test
    fun `дельты соответствуют экранной сетке`() {
        assertEquals(0, Direction.UP.deltaCol)
        assertEquals(-1, Direction.UP.deltaRow)

        assertEquals(0, Direction.DOWN.deltaCol)
        assertEquals(1, Direction.DOWN.deltaRow)

        assertEquals(-1, Direction.LEFT.deltaCol)
        assertEquals(0, Direction.LEFT.deltaRow)

        assertEquals(1, Direction.RIGHT.deltaCol)
        assertEquals(0, Direction.RIGHT.deltaRow)

        assertEquals(0, Direction.NONE.deltaCol)
        assertEquals(0, Direction.NONE.deltaRow)
    }

    @Test
    fun `opposite возвращает встречное направление`() {
        assertEquals(Direction.DOWN, Direction.UP.opposite())
        assertEquals(Direction.UP, Direction.DOWN.opposite())
        assertEquals(Direction.RIGHT, Direction.LEFT.opposite())
        assertEquals(Direction.LEFT, Direction.RIGHT.opposite())
        assertEquals(Direction.NONE, Direction.NONE.opposite())
    }

    @Test
    fun `opposite симметричен`() {
        Direction.entries.forEach { dir ->
            assertEquals(dir, dir.opposite().opposite(), "двойное opposite для $dir")
        }
    }

    @Test
    fun `fromKeys распознаёт одиночные клавиши`() {
        assertEquals(Direction.UP, Direction.fromKeys(up = true, down = false, left = false, right = false))
        assertEquals(Direction.DOWN, Direction.fromKeys(up = false, down = true, left = false, right = false))
        assertEquals(Direction.LEFT, Direction.fromKeys(up = false, down = false, left = true, right = false))
        assertEquals(Direction.RIGHT, Direction.fromKeys(up = false, down = false, left = false, right = true))
    }

    @Test
    fun `fromKeys без нажатий даёт NONE`() {
        assertEquals(Direction.NONE, Direction.fromKeys(up = false, down = false, left = false, right = false))
    }

    @Test
    fun `fromKeys приоритет up down left right`() {
        assertEquals(Direction.UP, Direction.fromKeys(up = true, down = true, left = true, right = true))
        assertEquals(Direction.DOWN, Direction.fromKeys(up = false, down = true, left = true, right = true))
        assertEquals(Direction.LEFT, Direction.fromKeys(up = false, down = false, left = true, right = true))
        assertEquals(Direction.RIGHT, Direction.fromKeys(up = false, down = false, left = false, right = true))
    }

    @Test
    fun `все дельты лежат в диапазоне -1 0 1`() {
        Direction.entries.forEach { dir ->
            assertFalse(dir.deltaCol < -1 || dir.deltaCol > 1, "deltaCol $dir")
            assertFalse(dir.deltaRow < -1 || dir.deltaRow > 1, "deltaRow $dir")
        }
    }
}
