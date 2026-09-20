package com.catsandmouses

import com.badlogic.gdx.math.GridPoint2
import com.catsandmouses.entities.Heart
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HeartTest : GdxTestBase() {

    @Test
    fun `позиция совпадает с центром клетки`() {
        val maze = MazeLoader("maps/open_room.txt")
        val cell = GridPoint2(3, 2)
        val heart = Heart(cell, maze)
        assertEquals(maze.cellToWorld(3, 2), heart.position)
        assertEquals(cell, heart.cell)
    }

    @Test
    fun `сердечко изначально не подобрано`() {
        val maze = MazeLoader("maps/open_room.txt")
        assertFalse(Heart(GridPoint2(1, 1), maze).collected)
    }

    @Test
    fun `collect помечает сердечко подобранным`() {
        val maze = MazeLoader("maps/open_room.txt")
        val heart = Heart(GridPoint2(1, 1), maze)
        heart.collect()
        assertTrue(heart.collected)
    }

    @Test
    fun `повторный collect идемпотентен`() {
        val maze = MazeLoader("maps/open_room.txt")
        val heart = Heart(GridPoint2(1, 1), maze)
        heart.collect()
        heart.collect()
        assertTrue(heart.collected)
    }

    @Test
    fun `размер спрайта 48`() {
        assertEquals(48f, Heart.SPRITE_SIZE)
    }
}
