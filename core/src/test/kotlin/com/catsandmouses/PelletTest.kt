package com.catsandmouses

import com.badlogic.gdx.math.GridPoint2
import com.catsandmouses.entities.Pellet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PelletTest : GdxTestBase() {

    @Test
    fun `позиция совпадает с центром клетки`() {
        val maze = MazeLoader("maps/open_room.txt")
        val cell = GridPoint2(3, 2)
        val pellet = Pellet(cell, maze)
        assertEquals(maze.cellToWorld(3, 2), pellet.position)
        assertEquals(cell, pellet.cell)
    }

    @Test
    fun `сыр изначально не съеден`() {
        val maze = MazeLoader("maps/open_room.txt")
        assertFalse(Pellet(GridPoint2(1, 1), maze).eaten)
    }

    @Test
    fun `eat помечает сыр съеденным`() {
        val maze = MazeLoader("maps/open_room.txt")
        val pellet = Pellet(GridPoint2(1, 1), maze)
        pellet.eat()
        assertTrue(pellet.eaten)
    }

    @Test
    fun `повторный eat идемпотентен`() {
        val maze = MazeLoader("maps/open_room.txt")
        val pellet = Pellet(GridPoint2(1, 1), maze)
        pellet.eat()
        pellet.eat()
        assertTrue(pellet.eaten)
    }

    @Test
    fun `размер спрайта 48`() {
        assertEquals(48f, Pellet.SPRITE_SIZE)
    }
}
