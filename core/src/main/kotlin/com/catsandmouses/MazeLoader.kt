package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.Vector2

class MazeLoader(filePath: String) {

    val width: Int
    val height: Int
    val grid: Array<CharArray>

    init {
        val lines = Gdx.files.internal(filePath)
            .readString()
            .split("\n")
            .map { it.trimEnd('\r') }
            .filter { it.isNotEmpty() }

        height = lines.size
        width = lines.maxOfOrNull { it.length } ?: 0

        grid = Array(height) { row ->
            CharArray(width) { col ->
                if (col < lines[row].length) lines[row][col] else '#'
            }
        }
    }

    fun isWall(col: Int, row: Int): Boolean {
        if (row < 0 || row >= height || col < 0 || col >= width) return true
        return grid[row][col] == '#'
    }

    fun isPassable(col: Int, row: Int): Boolean {
        return !isWall(col, row)
    }

    fun worldToCell(x: Float, y: Float): GridPoint2 {
        val col = ((x - Constants.MAZE_OFFSET_X) / Constants.CELL_SIZE).toInt()
        val row = (Constants.MAZE_ROWS - 1) - ((y - Constants.MAZE_OFFSET_Y) / Constants.CELL_SIZE).toInt()
        return GridPoint2(
            col.coerceIn(0, Constants.MAZE_COLS - 1),
            row.coerceIn(0, Constants.MAZE_ROWS - 1)
        )
    }

    fun cellToWorld(col: Int, row: Int): Vector2 {
        return Vector2(
            Constants.MAZE_OFFSET_X + col * Constants.CELL_SIZE + Constants.CELL_SIZE / 2f,
            Constants.MAZE_OFFSET_Y + (Constants.MAZE_ROWS - 1 - row) * Constants.CELL_SIZE + Constants.CELL_SIZE / 2f
        )
    }

    fun findPassableCell(preferCol: Int, preferRow: Int): GridPoint2 {
        if (isPassable(preferCol, preferRow)) {
            return GridPoint2(preferCol, preferRow)
        }
        for (row in 0 until height) {
            for (col in 0 until width) {
                if (isPassable(col, row)) return GridPoint2(col, row)
            }
        }
        return GridPoint2(1, 1)
    }

    fun findCheeseCells(): List<GridPoint2> {
        val result = mutableListOf<GridPoint2>()
        for (row in 0 until height) {
            for (col in 0 until width) {
                val ch = grid[row][col]
                if (ch in '1'..'9') {
                    result.add(GridPoint2(col, row))
                }
            }
        }
        return result
    }

    fun findReachableCells(startCol: Int, startRow: Int): List<GridPoint2> {
        val result = mutableListOf<GridPoint2>()
        val visited = mutableSetOf<GridPoint2>()
        val queue = ArrayDeque<GridPoint2>()
        val start = GridPoint2(startCol, startRow)

        if (!isPassable(startCol, startRow)) return result

        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result.add(current)

            for (dir in listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)) {
                val next = GridPoint2(current.x + dir.deltaCol, current.y + dir.deltaRow)
                if (isPassable(next.x, next.y) && !visited.contains(next)) {
                    visited.add(next)
                    queue.add(next)
                }
            }
        }

        return result
    }
}
