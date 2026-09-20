package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.Vector2

class MazeLoader private constructor(lines: List<String>) {

    val width: Int
    val height: Int
    val grid: Array<CharArray>

    constructor(filePath: String) : this(readLines(filePath))

    init {
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

    /**
     * Ищет явные стартовые клетки котов по буквам A..D.
     * Ключ — буква ('A'..'D'), значение — стартовая клетка соответствующего кота.
     */
    /** Клетки-сердечки: символ `H` (латинская, от HEART). */
    fun findHeartCells(): List<GridPoint2> {
        val result = mutableListOf<GridPoint2>()
        for (row in 0 until height) {
            for (col in 0 until width) {
                if (grid[row][col] == 'H') {
                    result.add(GridPoint2(col, row))
                }
            }
        }
        return result
    }

    fun findCatStarts(): Map<Char, GridPoint2> {
        val result = mutableMapOf<Char, GridPoint2>()
        for (row in 0 until height) {
            for (col in 0 until width) {
                val ch = grid[row][col]
                if (ch in 'A'..'D') {
                    result[ch] = GridPoint2(col, row)
                }
            }
        }
        return result
    }

    /**
     * Ищет стартовую клетку мыши.
     * Символ '+' — явная отметка; если таких несколько, выбирается случайный.
     * Если '+' нет, выбирается проходимая клетка, максимально близкая к центру карты.
     */
    fun findMouseStart(): GridPoint2 {
        val markedCells = mutableListOf<GridPoint2>()
        for (row in 0 until height) {
            for (col in 0 until width) {
                if (grid[row][col] == '+') {
                    markedCells.add(GridPoint2(col, row))
                }
            }
        }

        if (markedCells.isNotEmpty()) {
            val chosen = markedCells.random()
            // Если случайный '+' оказался в стене/недостижимым, ищем ближайший проходимый
            return if (isPassable(chosen.x, chosen.y)) chosen else findPassableCell(chosen.x, chosen.y)
        }

        val centerCol = width / 2
        val centerRow = height / 2
        val candidates = mutableListOf<GridPoint2>()
        for (row in 0 until height) {
            for (col in 0 until width) {
                if (isPassable(col, row)) {
                    candidates.add(GridPoint2(col, row))
                }
            }
        }
        candidates.sortBy { kotlin.math.abs(it.x - centerCol) + kotlin.math.abs(it.y - centerRow) }
        return candidates.firstOrNull() ?: GridPoint2(1, 1)
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

    companion object {
        /** Создать лабиринт из готового текста (тесты, dev-инструменты) — без чтения файла. */
        fun fromText(text: String): MazeLoader = MazeLoader(splitLines(text))

        private fun readLines(filePath: String): List<String> =
            splitLines(Gdx.files.internal(filePath).readString())

        private fun splitLines(text: String): List<String> =
            text.split("\n").map { it.trimEnd('\r') }.filter { it.isNotEmpty() }
    }
}
