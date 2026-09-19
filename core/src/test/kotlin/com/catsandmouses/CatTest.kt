package com.catsandmouses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.GridPoint2
import com.catsandmouses.entities.Cat
import com.catsandmouses.entities.CatStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CatTest : GdxTestBase() {

    private val openRoom: MazeLoader
        get() = MazeLoader("maps/open_room.txt")

    private fun cat(
        maze: MazeLoader = openRoom,
        strategy: CatStrategy = CatStrategy.Chaser,
        col: Int = 1,
        row: Int = 1,
        speed: Float = 120f
    ) = Cat(maze, col, row, strategy, Color.WHITE, "Тест", speed)

    private fun Cat.step(mouse: GridPoint2, direction: Direction = Direction.NONE, times: Int = 1, delta: Float = 0.5f) {
        repeat(times) { update(delta, mouse, direction, mutableSetOf()) }
    }

    @Test
    fun `начальное состояние`() {
        val cat = cat(col = 3, row = 2)
        assertEquals(GridPoint2(3, 2), cat.cell)
        assertEquals(openRoom.cellToWorld(3, 2), cat.position)
        assertEquals(Direction.NONE, cat.direction)
        assertEquals("Тест", cat.name)
        assertEquals(CatStrategy.Chaser, cat.strategy)
    }

    @Test
    fun `преследователь идёт к мыши`() {
        val mouse = GridPoint2(7, 3)
        val cat = cat()
        var steps = 0
        while (cat.cell != mouse && steps < 50) {
            cat.update(0.5f, mouse, Direction.NONE, mutableSetOf())
            steps++
        }
        assertEquals(mouse, cat.cell, "преследователь не дошёл до мыши за $steps шагов")
    }

    @Test
    fun `преследователь начинает движение в сторону мыши`() {
        val cat = cat(col = 1, row = 1)
        cat.step(GridPoint2(7, 1), times = 1)
        assertEquals(Direction.RIGHT, cat.direction)
    }

    @Test
    fun `преследователь не разворачивается в коридоре`() {
        val maze = MazeLoader("maps/corridor.txt")
        val mouse = GridPoint2(5, 1)
        val cat = cat(maze = maze, col = 1, row = 1)

        var previous = Direction.NONE
        repeat(4) {
            cat.update(0.5f, mouse, Direction.NONE, mutableSetOf())
            assertTrue(
                cat.direction != previous.opposite() || previous == Direction.NONE,
                "кот развернулся: $previous -> ${cat.direction}"
            )
            previous = cat.direction
        }
        assertEquals(Direction.RIGHT, cat.direction)
    }

    @Test
    fun `кот разворачивается в тупике`() {
        val maze = MazeLoader("maps/dead_end_corridor.txt")
        val cat = cat(maze = maze, col = 1, row = 1)
        val mouse = GridPoint2(3, 1)

        cat.update(0.5f, mouse, Direction.NONE, mutableSetOf()) // старт вправо
        cat.update(0.5f, mouse, Direction.NONE, mutableSetOf()) // (2,1)
        cat.update(0.5f, mouse, Direction.NONE, mutableSetOf()) // (3,1) — тупик

        assertEquals(GridPoint2(3, 1), cat.cell)
        assertEquals(Direction.LEFT, cat.direction, "в тупике кот должен развернуться")
    }

    @Test
    fun `кот обходит зарезервированные клетки`() {
        val cat = cat(col = 1, row = 1)
        val reserved = mutableSetOf(2 to 1) // клетка вправо занята
        cat.update(0.5f, GridPoint2(7, 1), Direction.NONE, reserved)
        assertEquals(Direction.DOWN, cat.direction)
    }

    @Test
    fun `кот вынужден двинуться если все пути зарезервированы`() {
        val cat = cat(col = 1, row = 1)
        val mouse = GridPoint2(7, 1)
        val reserved = mutableSetOf(1 to 2, 2 to 1) // оба прохода заняты

        cat.update(0.5f, mouse, Direction.NONE, reserved)
        cat.update(0.5f, mouse, Direction.NONE, reserved)
        cat.update(0.5f, mouse, Direction.NONE, reserved)
        assertEquals(Direction.NONE, cat.direction, "пока не прошла секунда — кот ждёт")

        cat.update(0.5f, mouse, Direction.NONE, reserved)
        assertEquals(Direction.RIGHT, cat.direction, "после секунды ожидания — форс-мув")
    }

    @Test
    fun `засадный кот целится с опережением по направлению мыши`() {
        val ambush = cat(strategy = CatStrategy.Ambush, col = 3, row = 3)
        val chaser = cat(strategy = CatStrategy.Chaser, col = 3, row = 3)

        ambush.update(0.5f, GridPoint2(5, 3), Direction.UP, mutableSetOf())
        chaser.update(0.5f, GridPoint2(5, 3), Direction.NONE, mutableSetOf())

        assertEquals(Direction.UP, ambush.direction, "цель — клетка выше мыши")
        assertEquals(Direction.RIGHT, chaser.direction, "обычный преследователь идёт прямо к мыши")
    }

    @Test
    fun `при отсутствии направления мыши засадный кот идёт к ней`() {
        val ambush = cat(strategy = CatStrategy.Ambush, col = 3, row = 3)
        ambush.update(0.5f, GridPoint2(5, 3), Direction.NONE, mutableSetOf())
        assertEquals(Direction.RIGHT, ambush.direction)
    }

    @Test
    fun `патрульный кот преследует вблизи`() {
        val patrol = cat(strategy = CatStrategy.Patrol, col = 3, row = 3)
        patrol.update(0.5f, GridPoint2(5, 3), Direction.NONE, mutableSetOf())
        assertEquals(Direction.RIGHT, patrol.direction)
    }

    @Test
    fun `патрульный кот вдали держится у стен`() {
        val patrol = cat(strategy = CatStrategy.Patrol, col = 1, row = 1)
        patrol.update(0.5f, GridPoint2(7, 3), Direction.NONE, mutableSetOf())
        assertTrue(
            patrol.direction == Direction.DOWN || patrol.direction == Direction.RIGHT,
            "ожидается движение вдоль стены, получено ${patrol.direction}"
        )
    }

    @Test
    fun `случайный кот выбирает только достижимое направление`() {
        val cat = cat(strategy = CatStrategy.Random, col = 4, row = 2)
        cat.update(0.5f, GridPoint2(7, 1), Direction.NONE, mutableSetOf())

        assertTrue(cat.direction != Direction.NONE, "кот в открытом помещении должен двинуться")
        val targetCol = cat.cell.x + cat.direction.deltaCol
        val targetRow = cat.cell.y + cat.direction.deltaRow
        assertTrue(openRoom.isPassable(targetCol, targetRow), "выбран проход в стену: ${cat.direction}")
    }

    @Test
    fun `скорость кота влияет на смещение за кадр`() {
        val cat = cat(col = 1, row = 2, speed = 200f)
        cat.update(0.5f, GridPoint2(7, 2), Direction.NONE, mutableSetOf()) // старт
        cat.update(0.1f, GridPoint2(7, 2), Direction.NONE, mutableSetOf()) // 200 * 0.1 = 20 px
        assertEquals(openRoom.cellToWorld(1, 2).x + 20f, cat.position.x, 0.01f)
    }

    @Test
    fun `константы кота`() {
        assertEquals(48f, Cat.SPRITE_SIZE)
        assertEquals(90f, Cat.SPRITE_ROTATION_OFFSET)
    }
}
