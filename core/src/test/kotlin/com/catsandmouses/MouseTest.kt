package com.catsandmouses

import com.badlogic.gdx.math.GridPoint2
import com.catsandmouses.entities.Mouse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MouseTest : GdxTestBase() {

    private val maze: MazeLoader
        get() = MazeLoader("maps/open_room.txt")

    private fun Mouse.step(times: Int, delta: Float = 0.3f) {
        repeat(times) { update(delta) }
    }

    @Test
    fun `начальное состояние`() {
        val mouse = Mouse(maze, 1, 2)
        assertEquals(GridPoint2(1, 2), mouse.cell)
        assertEquals(maze.cellToWorld(1, 2), mouse.position)
        assertEquals(Direction.NONE, mouse.direction)
    }

    @Test
    fun `без ввода мышь не двигается`() {
        val mouse = Mouse(maze, 1, 2)
        mouse.step(5)
        assertEquals(GridPoint2(1, 2), mouse.cell)
        assertEquals(Direction.NONE, mouse.direction)
    }

    @Test
    fun `движение вправо`() {
        val mouse = Mouse(maze, 1, 2)
        mouse.setNextDirection(Direction.RIGHT)

        mouse.update(0.3f) // старт движения, но клетка ещё не пройдена
        assertEquals(Direction.RIGHT, mouse.direction)
        assertEquals(GridPoint2(1, 2), mouse.cell)

        mouse.update(0.3f) // проходим клетку
        assertEquals(GridPoint2(2, 2), mouse.cell)
    }

    @Test
    fun `в стену движение не начинается`() {
        val mouse = Mouse(maze, 1, 1)
        mouse.setNextDirection(Direction.UP) // (1,0) — стена
        mouse.update(0.3f)
        assertEquals(Direction.NONE, mouse.direction)
        assertEquals(GridPoint2(1, 1), mouse.cell)
    }

    @Test
    fun `поворот происходит только в центре клетки`() {
        val mouse = Mouse(maze, 1, 2)
        mouse.setNextDirection(Direction.RIGHT)
        mouse.update(0.3f) // старт

        // Просим повернуть вверх в середине клетки
        mouse.setNextDirection(Direction.UP)
        mouse.update(0.1f)
        assertEquals(Direction.RIGHT, mouse.direction, "в середине клетки поворот невозможен")
        assertEquals(GridPoint2(1, 2), mouse.cell)
        assertEquals(maze.cellToWorld(1, 2).x + 15f, mouse.position.x, 0.01f)

        // Дошли до центра следующей клетки — теперь поворот срабатывает
        mouse.update(0.3f)
        assertEquals(GridPoint2(2, 2), mouse.cell)
        assertEquals(Direction.UP, mouse.direction)
    }

    @Test
    fun `мышь останавливается у стены`() {
        val mouse = Mouse(maze, 1, 2)
        mouse.setNextDirection(Direction.RIGHT)
        mouse.step(times = 30)
        assertEquals(GridPoint2(7, 2), mouse.cell)
        assertEquals(Direction.NONE, mouse.direction)
        assertEquals(maze.cellToWorld(7, 2), mouse.position)
    }

    @Test
    fun `продолжает движение по инерции без нового ввода`() {
        val mouse = Mouse(maze, 1, 2)
        mouse.setNextDirection(Direction.RIGHT)
        mouse.step(times = 3)
        assertEquals(GridPoint2(3, 2), mouse.cell)
        assertEquals(Direction.RIGHT, mouse.direction)
    }

    @Test
    fun `NONE не сбрасывает заданное направление`() {
        val mouse = Mouse(maze, 1, 2)
        mouse.setNextDirection(Direction.RIGHT)
        mouse.update(0.3f)
        mouse.setNextDirection(Direction.NONE)
        mouse.update(0.3f)
        assertEquals(GridPoint2(2, 2), mouse.cell)
        assertEquals(Direction.RIGHT, mouse.direction)
    }

    @Test
    fun `скорость мыши 150 пикселей в секунду`() {
        val mouse = Mouse(maze, 1, 2)
        mouse.setNextDirection(Direction.RIGHT)
        mouse.update(0.3f) // старт
        mouse.update(0.1f) // 150 * 0.1 = 15 px
        assertEquals(maze.cellToWorld(1, 2).x + 15f, mouse.position.x, 0.01f)
    }

    @Test
    fun `константы мыши`() {
        assertEquals(15f, Mouse.RADIUS)
        assertEquals(48f, Mouse.SPRITE_SIZE)
        assertEquals(90f, Mouse.SPRITE_ROTATION_OFFSET)
    }
}
