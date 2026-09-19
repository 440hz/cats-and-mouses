package com.catsandmouses

import com.badlogic.gdx.math.GridPoint2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MazeLoaderTest : GdxTestBase() {

    @Test
    fun `реальная карта читается как 32 на 16`() {
        val maze = MazeLoader("maps/1/1.txt")
        assertEquals(32, maze.width)
        assertEquals(16, maze.height)
        assertEquals(16, maze.grid.size)
        assertEquals(32, maze.grid[0].size)
    }

    @Test
    fun `периметр реальной карты это стены`() {
        val maze = MazeLoader("maps/1/1.txt")
        for (col in 0 until maze.width) {
            assertTrue(maze.isWall(col, 0), "верх $col")
            assertTrue(maze.isWall(col, maze.height - 1), "низ $col")
        }
        for (row in 0 until maze.height) {
            assertTrue(maze.isWall(0, row), "лево $row")
            assertTrue(maze.isWall(maze.width - 1, row), "право $row")
        }
    }

    @Test
    fun `короткие строки добиваются стеной`() {
        val maze = MazeLoader("maps/pad_short_rows.txt")
        assertEquals(6, maze.width)
        assertEquals(5, maze.height)

        // Строка "##" добита стенами до ширины 6
        for (col in 0 until maze.width) {
            assertTrue(maze.isWall(col, 2), "col=$col должен быть стеной")
        }
        // Строка "#   #" (5 символов) добита стеной в col=5
        assertEquals('#', maze.grid[1][5])
        assertTrue(maze.isPassable(3, 1))
    }

    @Test
    fun `CRLF и пустые строки обрабатываются`() {
        val maze = MazeLoader("maps/crlf.txt")
        assertEquals(3, maze.width)
        assertEquals(3, maze.height)
        assertTrue(maze.isWall(0, 0))
        assertTrue(maze.isPassable(1, 1))
        assertTrue(maze.isWall(0, 2))
    }

    @Test
    fun `isWall за границами карты`() {
        val maze = MazeLoader("maps/mouse_center.txt")
        assertTrue(maze.isWall(-1, 0))
        assertTrue(maze.isWall(0, -1))
        assertTrue(maze.isWall(maze.width, 0))
        assertTrue(maze.isWall(0, maze.height))
        assertFalse(maze.isPassable(-1, -1))
    }

    @Test
    fun `isPassable обратен isWall`() {
        val maze = MazeLoader("maps/entities.txt")
        for (row in 0 until maze.height) {
            for (col in 0 until maze.width) {
                assertEquals(!maze.isWall(col, row), maze.isPassable(col, row), "($col,$row)")
            }
        }
    }

    @Test
    fun `worldToCell и cellToWorld взаимно обратны`() {
        val maze = MazeLoader("maps/1/1.txt")
        for (row in 0 until maze.height) {
            for (col in 0 until maze.width) {
                val world = maze.cellToWorld(col, row)
                val cell = maze.worldToCell(world.x, world.y)
                assertEquals(col, cell.x, "col при ($col,$row)")
                assertEquals(row, cell.y, "row при ($col,$row)")
            }
        }
    }

    @Test
    fun `worldToCell клампится по границам`() {
        val maze = MazeLoader("maps/1/1.txt")
        assertEquals(GridPoint2(0, 15), maze.worldToCell(-100f, -100f))
        assertEquals(GridPoint2(31, 0), maze.worldToCell(10000f, 10000f))
    }

    @Test
    fun `cellToWorld центрирует клетку`() {
        val maze = MazeLoader("maps/1/1.txt")
        val world = maze.cellToWorld(0, 0)
        assertEquals(Constants.MAZE_OFFSET_X + Constants.CELL_SIZE / 2f, world.x)
        // row 0 — верхняя строка, значит максимальная Y
        assertEquals(
            Constants.MAZE_OFFSET_Y + (Constants.MAZE_ROWS - 1) * Constants.CELL_SIZE + Constants.CELL_SIZE / 2f,
            world.y
        )
    }

    @Test
    fun `findPassableCell возвращает саму клетку если она проходима`() {
        val maze = MazeLoader("maps/mouse_center.txt")
        assertEquals(GridPoint2(2, 2), maze.findPassableCell(2, 2))
    }

    @Test
    fun `findPassableCell ищет первую проходимую при стене`() {
        val maze = MazeLoader("maps/mouse_center.txt")
        assertEquals(GridPoint2(1, 1), maze.findPassableCell(0, 0))
    }

    @Test
    fun `findPassableCell на карте из стен даёт fallback`() {
        val maze = MazeLoader("maps/all_walls.txt")
        assertEquals(GridPoint2(1, 1), maze.findPassableCell(0, 0))
    }

    @Test
    fun `findCheeseCells находит цифры`() {
        val maze = MazeLoader("maps/entities.txt")
        assertEquals(
            listOf(GridPoint2(3, 1), GridPoint2(1, 3), GridPoint2(7, 3)),
            maze.findCheeseCells()
        )
    }

    @Test
    fun `findCatStarts по буквам A D`() {
        val maze = MazeLoader("maps/entities.txt")
        val starts = maze.findCatStarts()
        assertEquals(setOf('A', 'B', 'C', 'D'), starts.keys)
        assertEquals(GridPoint2(1, 1), starts.getValue('A'))
        assertEquals(GridPoint2(7, 1), starts.getValue('B'))
        assertEquals(GridPoint2(1, 4), starts.getValue('C'))
        assertEquals(GridPoint2(8, 4), starts.getValue('D'))
    }

    @Test
    fun `findMouseStart по символу плюс`() {
        val maze = MazeLoader("maps/entities.txt")
        val start = maze.findMouseStart()
        assertTrue(listOf(GridPoint2(3, 2), GridPoint2(4, 2), GridPoint2(5, 2)).contains(start), "получено $start")
        assertTrue(maze.isPassable(start.x, start.y))
    }

    @Test
    fun `findMouseStart без плюса выбирает центр`() {
        val maze = MazeLoader("maps/mouse_center.txt")
        assertEquals(GridPoint2(4, 2), maze.findMouseStart())
    }

    @Test
    fun `findReachableCells обходит всё проходимое пространство`() {
        val maze = MazeLoader("maps/entities.txt")
        val reachable = maze.findReachableCells(3, 2)
        assertEquals(32, reachable.size)
        assertTrue(reachable.contains(GridPoint2(1, 1)))
        assertTrue(reachable.contains(GridPoint2(8, 4)))
        assertFalse(reachable.any { it.x == 0 || it.x == 9 || it.y == 0 || it.y == 5 })
    }

    @Test
    fun `findReachableCells не переходит между изолированными областями`() {
        val maze = MazeLoader("maps/reachable_islands.txt")
        val left = maze.findReachableCells(2, 1)
        assertEquals(6, left.size)
        assertFalse(left.any { it.x >= 5 }, "правая область не должна быть достижима")
    }

    @Test
    fun `findReachableCells из стены пуст`() {
        val maze = MazeLoader("maps/reachable_islands.txt")
        assertTrue(maze.findReachableCells(0, 0).isEmpty())
        assertTrue(maze.findReachableCells(4, 1).isEmpty())
    }
}
