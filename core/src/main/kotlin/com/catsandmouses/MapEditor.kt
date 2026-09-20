package com.catsandmouses

import com.badlogic.gdx.math.GridPoint2

/**
 * Состояние редактора карты: сетка [height]×[width], история правок и сериализация.
 * Чистая логика без GL — пригоден для unit-тестов.
 *
 * Символы карты те же, что у [MazeLoader]:
 * `#` — стена, ` ` — проход, `1`–`9` — сыр, `H` — сердечко, `A`–`D` — старты котов, `+` — старт мыши.
 */
class MapEditor(
    val width: Int = Constants.MAZE_COLS,
    val height: Int = Constants.MAZE_ROWS,
    private val undoLimit: Int = 200
) {

    /** Текущая сетка: `grid[row][col]`. */
    val grid: Array<CharArray> = Array(height) { CharArray(width) { '#' } }

    private val undoStack = ArrayDeque<Array<CharArray>>()
    private val redoStack = ArrayDeque<Array<CharArray>>()

    /** Есть ли несохранённые изменения. */
    var dirty = false
        private set

    /** Загрузить карту из текста (16 строк по 32 символа; короткие строки добиваются стенами). */
    fun load(text: String) {
        val lines = text
            .split("\n")
            .map { it.trimEnd('\r') }
            .filter { it.isNotEmpty() }

        for (row in 0 until height) {
            val line = lines.getOrNull(row) ?: ""
            for (col in 0 until width) {
                grid[row][col] = if (col < line.length) line[col] else '#'
            }
        }

        undoStack.clear()
        redoStack.clear()
        dirty = false
    }

    /** Сериализовать карту: ровно [height] строк по [width] символов + финальный `\n`. */
    fun toText(): String = buildString {
        for (row in 0 until height) {
            append(grid[row])
            append('\n')
        }
    }

    /** Поставить символ в клетку. Вне границ или недопустимый символ — игнорируется. */
    fun setCell(col: Int, row: Int, ch: Char) {
        if (!inBounds(col, row) || ch !in ALLOWED) return
        if (grid[row][col] == ch) return
        pushUndo()
        grid[row][col] = ch
        dirty = true
    }

    /** Залить прямоугольник по двум углам (координаты нормализуются и клампятся). */
    fun fillRect(col1: Int, row1: Int, col2: Int, row2: Int, ch: Char) {
        if (ch !in ALLOWED) return
        val left = minOf(col1, col2).coerceIn(0, width - 1)
        val right = maxOf(col1, col2).coerceIn(0, width - 1)
        val top = minOf(row1, row2).coerceIn(0, height - 1)
        val bottom = maxOf(row1, row2).coerceIn(0, height - 1)

        var changed = false
        for (row in top..bottom) {
            for (col in left..right) {
                if (grid[row][col] != ch) {
                    changed = true
                }
            }
        }
        if (!changed) return

        pushUndo()
        for (row in top..bottom) {
            for (col in left..right) {
                grid[row][col] = ch
            }
        }
        dirty = true
    }

    /** Число клеток-сыров на карте. */
    fun cheeseCount(): Int {
        var count = 0
        for (row in 0 until height) {
            for (col in 0 until width) {
                if (grid[row][col] in '1'..'9') count++
            }
        }
        return count
    }

    /**
     * Поставить старт кота (`A`–`D`). На карте допускается ровно один старт на букву:
     * прежняя клетка этой буквы стирается (перенос), дубликаты не создаются.
     */
    fun setCatStart(col: Int, row: Int, ch: Char) {
        if (ch !in 'A'..'D' || !inBounds(col, row)) return
        if (grid[row][col] == ch) return

        pushUndo()
        // Ровно один старт на букву: прежнюю клетку стираем (перенос)
        for (r in 0 until height) {
            for (c in 0 until width) {
                if (grid[r][c] == ch) grid[r][c] = ' '
            }
        }
        grid[row][col] = ch
        dirty = true
    }

    /** Число клеток-сердечек на карте (символ `H`). */
    fun heartCount(): Int {
        var count = 0
        for (row in 0 until height) {
            for (col in 0 until width) {
                if (grid[row][col] == HEART) count++
            }
        }
        return count
    }

    /**
     * Перенумеровать сыр как последовательность `1..N` в порядке обхода (строка за строкой).
     * Значение цифры игре не важно — нормализация только для читаемости формата.
     */
    fun normalizeCheese() {
        var next = 1
        for (row in 0 until height) {
            for (col in 0 until width) {
                if (grid[row][col] in '1'..'9') {
                    if (next <= 9) grid[row][col] = ('0' + next)
                    next++
                }
            }
        }
    }

    /**
     * Залить сыром пустые проходимые клетки (`' '`) прямоугольника одним шагом undo.
     * Клетки `#`, `+`, `A`–`D` и уже стоящий сыр не трогаются.
     * @return false (без изменений), если итоговое число сыров превысит [limit].
     */
    fun fillCheeseRect(col1: Int, row1: Int, col2: Int, row2: Int, limit: Int): Boolean {
        val left = minOf(col1, col2).coerceIn(0, width - 1)
        val right = maxOf(col1, col2).coerceIn(0, width - 1)
        val top = minOf(row1, row2).coerceIn(0, height - 1)
        val bottom = maxOf(row1, row2).coerceIn(0, height - 1)

        var empties = 0
        for (row in top..bottom) {
            for (col in left..right) {
                if (grid[row][col] == ' ') empties++
            }
        }
        if (cheeseCount() + empties > limit) return false
        if (empties == 0) return true

        pushUndo()
        for (row in top..bottom) {
            for (col in left..right) {
                if (grid[row][col] == ' ') grid[row][col] = '1'
            }
        }
        dirty = true
        return true
    }

    /**
     * Залить сердечками (`H`) пустые проходимые клетки (`' '`) прямоугольника одним шагом undo.
     * Лимит [limit] — общий максимум сердечек на карте: лишние клетки **игнорируются**
     * (ставим сколько влезает, а не отменяем заливку).
     * @return сколько сердечек фактически поставлено.
     */
    fun fillHeartRect(col1: Int, row1: Int, col2: Int, row2: Int, limit: Int): Int {
        val left = minOf(col1, col2).coerceIn(0, width - 1)
        val right = maxOf(col1, col2).coerceIn(0, width - 1)
        val top = minOf(row1, row2).coerceIn(0, height - 1)
        val bottom = maxOf(row1, row2).coerceIn(0, height - 1)

        val remaining = (limit - heartCount()).coerceAtLeast(0)
        if (remaining == 0) return 0

        val targets = mutableListOf<Pair<Int, Int>>()
        for (row in top..bottom) {
            for (col in left..right) {
                if (grid[row][col] == ' ') targets.add(col to row)
            }
        }
        val toPlace = minOf(remaining, targets.size)
        if (toPlace == 0) return 0

        pushUndo()
        for (i in 0 until toPlace) {
            val (col, row) = targets[i]
            grid[row][col] = HEART
        }
        dirty = true
        return toPlace
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(snapshot())
        restore(previous)
        dirty = true
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(snapshot())
        restore(next)
        dirty = true
    }

    /** Отметить состояние как сохранённое. */
    fun markSaved() {
        dirty = false
    }

    /** Результат проверки карты на пригодность для игры. */
    data class Validation(
        val cheeseCount: Int,
        val cheeseReachable: Boolean,
        val unreachableCheese: List<GridPoint2>,
        val mouseStartCount: Int,
        val catStarts: Set<Char>
    ) {
        val hasMouseStart: Boolean get() = mouseStartCount > 0
        val hasCheese: Boolean get() = cheeseCount > 0
        val hasAllCats: Boolean get() = catStarts.containsAll(CAT_LETTERS)
        val isOk: Boolean
            get() = hasMouseStart && hasCheese && hasAllCats && unreachableCheese.isEmpty()

        /** Человекочитаемые предупреждения для статус-строки редактора. */
        fun warnings(): List<String> = buildList {
            if (!hasMouseStart) add("нет старта мыши (+)")
            if (!hasCheese) add("нет сыра")
            if (!hasAllCats) add("не все старты котов (A-D)")
            if (unreachableCheese.isNotEmpty()) add("недостижимый сыр: " + unreachableCheese.joinToString { "(${it.x},${it.y})" })
        }

        companion object {
            val CAT_LETTERS = setOf('A', 'B', 'C', 'D')
        }
    }

    /** Проверка текущей карты (достижимость сыра, старты). */
    fun validate(): Validation {
        val maze = MazeLoader.fromText(toText())

        var cheeseCount = 0
        var mouseStartCount = 0
        val catStarts = mutableSetOf<Char>()
        for (row in 0 until height) {
            for (col in 0 until width) {
                when (val ch = grid[row][col]) {
                    in '1'..'9' -> cheeseCount++
                    '+' -> mouseStartCount++
                    in 'A'..'D' -> catStarts.add(ch)
                }
            }
        }

        val start = maze.findMouseStart()
        val reachable = maze.findReachableCells(start.x, start.y).toSet()
        val unreachableCheese = maze.findCheeseCells().filter { it !in reachable }

        return Validation(
            cheeseCount = cheeseCount,
            cheeseReachable = cheeseCount > 0 && unreachableCheese.isEmpty(),
            unreachableCheese = unreachableCheese,
            mouseStartCount = mouseStartCount,
            catStarts = catStarts
        )
    }

    private fun inBounds(col: Int, row: Int): Boolean =
        col in 0 until width && row in 0 until height

    private fun snapshot(): Array<CharArray> = Array(height) { grid[it].copyOf() }

    private fun restore(source: Array<CharArray>) {
        for (row in 0 until height) {
            grid[row] = source[row].copyOf()
        }
    }

    private fun pushUndo() {
        undoStack.addLast(snapshot())
        while (undoStack.size > undoLimit) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    companion object {
        /** Символ сердечка (латинская H, от HEART). */
        const val HEART = 'H'

        /** Допустимые символы карты. */
        val ALLOWED: Set<Char> = buildSet {
            add('#')
            add(' ')
            add('+')
            add(HEART)
            for (ch in '1'..'9') add(ch)
            for (ch in 'A'..'D') add(ch)
        }
    }
}
