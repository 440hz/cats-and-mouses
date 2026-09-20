package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.GridPoint2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

class MapEditorTest : GdxTestBase() {

    private fun editorOf(vararg rows: String): MapEditor {
        val width = rows.maxOf { it.length }
        val editor = MapEditor(width, rows.size)
        editor.load(rows.joinToString("\n") + "\n")
        return editor
    }

    private fun MapEditor.row(row: Int): String = String(grid[row])

    // ---------- I/O и нормализация ----------

    @Test
    fun `round-trip всех реальных карт байт-в-байт`() {
        val root = System.getProperty("project.root")
        assumeTrue(root != null, "нет project.root")
        val mapsDir = File(root, "assets/maps")
        assumeTrue(mapsDir.isDirectory, "нет папки ${mapsDir.path}")

        val files = mapsDir.walkTopDown().filter { it.isFile && it.extension == "txt" }.toList()
        assumeTrue(files.isNotEmpty(), "нет карт")

        for (file in files) {
            val original = file.readText()
            val editor = MapEditor()
            editor.load(original)
            assertEquals(original, editor.toText(), "round-trip файла ${file.name}")
        }
    }

    @Test
    fun `toText всегда 16 строк по 32 символа`() {
        val editor = MapEditor()
        editor.load(Gdx.files.internal("maps/1/1.txt").readString())
        val lines = editor.toText().trimEnd('\n').split('\n')
        assertEquals(Constants.MAZE_ROWS, lines.size)
        for (line in lines) assertEquals(Constants.MAZE_COLS, line.length)
    }

    @Test
    fun `CRLF и финальный перевод строки обрабатываются`() {
        val text = Gdx.files.internal("maps/crlf.txt").readString()
        val editor = MapEditor(3, 3)
        editor.load(text)
        assertEquals("###", editor.row(0))
        assertEquals("# #", editor.row(1))
        assertEquals("###", editor.row(2))
        assertEquals("###\n# #\n###\n", editor.toText())
    }

    @Test
    fun `короткие строки добиваются стеной`() {
        val editor = MapEditor(6, 2)
        editor.load("##\n#\n")
        assertEquals("######", editor.row(0))
        // Строка "#" добита стенами до ширины 6
        assertEquals("######", editor.row(1))
    }

    @Test
    fun `длинные строки обрезаются`() {
        val editor = MapEditor(3, 1)
        editor.load("#####\n")
        assertEquals("###", editor.row(0))
    }

    @Test
    fun `карта из стен и карта из проходов сохраняются`() {
        val walls = MapEditor(3, 2)
        walls.load("###\n###\n")
        assertEquals("###\n###\n", walls.toText())
        assertTrue(walls.grid.all { row -> String(row) == "###" })

        val open = MapEditor(3, 2)
        open.load("   \n   \n")
        assertEquals("   \n   \n", open.toText())
    }

    @Test
    fun `пробелы-проходы сохраняются включая конец строки`() {
        val editor = MapEditor(3, 2)
        editor.load("   \n# #\n")
        assertEquals("   ", editor.row(0))
        assertEquals("# #", editor.row(1))
        assertEquals("   \n# #\n", editor.toText())
    }

    // ---------- Операции редактирования ----------

    @Test
    fun `недопустимые символы игнорируются`() {
        val editor = MapEditor(3, 3)
        editor.setCell(1, 1, 'X')
        assertEquals('#', editor.grid[1][1])
        editor.fillRect(0, 0, 2, 2, 'x')
        assertTrue(editor.grid.all { row -> String(row) == "###" })
    }

    @Test
    fun `setCell ставит символ и игнорирует выход за границы`() {
        val editor = MapEditor(3, 3)
        editor.setCell(1, 1, ' ')
        assertEquals(' ', editor.grid[1][1])
        assertTrue(editor.dirty)

        editor.setCell(-1, 0, ' ')
        editor.setCell(0, -1, ' ')
        editor.setCell(3, 0, ' ')
        editor.setCell(0, 3, ' ')
        assertEquals("###", editor.row(0))
        assertEquals(1, (0 until 3).sumOf { r -> editor.grid[r].count { it != '#' } })
    }

    @Test
    fun `fillRect заливает прямоугольник в границах`() {
        val editor = MapEditor(5, 5)
        editor.fillRect(1, 1, 3, 3, ' ')
        for (row in 0 until 5) {
            for (col in 0 until 5) {
                val inside = row in 1..3 && col in 1..3
                assertEquals(if (inside) ' ' else '#', editor.grid[row][col], "($col,$row)")
            }
        }
    }

    @Test
    fun `fillRect с обратными углами нормализуется`() {
        val a = MapEditor(5, 5)
        a.fillRect(1, 1, 3, 3, ' ')
        val b = MapEditor(5, 5)
        b.fillRect(3, 3, 1, 1, ' ')
        for (row in 0 until 5) assertEquals(a.row(row), b.row(row))
    }

    @Test
    fun `normalizeCheese нумерует сыр последовательно`() {
        val editor = MapEditor(4, 1)
        editor.load("3344\n")
        assertEquals(4, editor.cheeseCount())
        editor.normalizeCheese()
        assertEquals("1234", String(editor.grid[0]))
    }

    @Test
    fun `normalizeCheese не трогает прочие символы`() {
        val editor = MapEditor(4, 1)
        editor.load("+A #\n")
        editor.normalizeCheese()
        assertEquals("+A #", String(editor.grid[0]))
        assertEquals(0, editor.cheeseCount())
    }

    @Test
    fun `fillCheeseRect заливает только пустые клетки`() {
        val editor = MapEditor(3, 2)
        editor.load("   \n# #\n")
        assertTrue(editor.fillCheeseRect(0, 0, 2, 1, limit = 9))
        assertEquals(4, editor.cheeseCount())
        assertEquals("111", editor.row(0))
        assertEquals("#1#", editor.row(1))
    }

    @Test
    fun `fillCheeseRect не превышает лимит`() {
        val editor = MapEditor(3, 2)
        editor.load("   \n# #\n")
        assertFalse(editor.fillCheeseRect(0, 0, 2, 1, limit = 2))
        assertEquals(0, editor.cheeseCount())
        assertEquals("   ", editor.row(0))
    }

    @Test
    fun `undo и redo возвращают состояние`() {
        val editor = MapEditor(3, 3)
        editor.setCell(1, 1, ' ')
        assertEquals(' ', editor.grid[1][1])

        editor.undo()
        assertEquals('#', editor.grid[1][1])

        editor.redo()
        assertEquals(' ', editor.grid[1][1])
    }

    @Test
    fun `undo на пустом стеке не падает и не меняет карту`() {
        val editor = MapEditor(3, 3)
        editor.undo()
        editor.redo()
        assertTrue(editor.grid.all { row -> String(row) == "###" })
    }

    @Test
    fun `новая правка очищает redo`() {
        val editor = MapEditor(3, 3)
        editor.setCell(1, 1, ' ')
        editor.undo()
        editor.setCell(1, 1, '1')
        editor.redo()
        assertEquals('1', editor.grid[1][1])
    }

    @Test
    fun `лимит истории undo соблюдается`() {
        val editor = MapEditor(1, 1, undoLimit = 2)
        editor.setCell(0, 0, ' ')
        editor.setCell(0, 0, '1')
        editor.setCell(0, 0, '2')

        editor.undo()
        assertEquals('1', editor.grid[0][0])
        editor.undo()
        assertEquals(' ', editor.grid[0][0])
        editor.undo() // история исчерпана — no-op
        assertEquals(' ', editor.grid[0][0])
    }

    // ---------- Старты котов ----------

    @Test
    fun `setCatStart переносит кота без дубликатов`() {
        val editor = MapEditor(5, 1)
        editor.load("     \n")
        editor.setCatStart(1, 0, 'A')
        assertEquals(" A   ", String(editor.grid[0]))

        editor.setCatStart(3, 0, 'A')
        assertEquals("   A ", String(editor.grid[0]))
        assertEquals(1, (0 until 5).count { editor.grid[0][it] == 'A' })
    }

    @Test
    fun `setCatStart различает буквы`() {
        val editor = MapEditor(5, 1)
        editor.load("     \n")
        editor.setCatStart(0, 0, 'A')
        editor.setCatStart(1, 0, 'B')
        editor.setCatStart(2, 0, 'C')
        editor.setCatStart(3, 0, 'D')
        assertEquals("ABCD ", String(editor.grid[0]))
    }

    @Test
    fun `setCatStart игнорирует не буквы котов и выход за границы`() {
        val editor = MapEditor(3, 1)
        editor.load("   \n")
        editor.setCatStart(1, 0, 'E')
        editor.setCatStart(1, 0, '1')
        editor.setCatStart(5, 0, 'A')
        editor.setCatStart(0, 5, 'A')
        assertEquals("   ", String(editor.grid[0]))
    }

    // ---------- Сердечки ----------

    @Test
    fun `H допустим и не считается сыром`() {
        val editor = MapEditor(4, 1)
        editor.load("3H4H\n")
        assertEquals(2, editor.cheeseCount())
        assertEquals(2, editor.heartCount())

        // Сыр перенумеровывается на месте (3,4 → 1,2), сердечки не трогаются
        editor.normalizeCheese()
        assertEquals("1H2H", String(editor.grid[0]))
        assertEquals(2, editor.heartCount())
    }

    @Test
    fun `setCell ставит сердечко`() {
        val editor = MapEditor(3, 3)
        editor.setCell(1, 1, 'H')
        assertEquals('H', editor.grid[1][1])
        assertEquals(1, editor.heartCount())
    }

    @Test
    fun `fillHeartRect ставит до лимита и игнорирует лишнее`() {
        val editor = MapEditor(3, 2)
        editor.load("   \n# #\n")
        val placed = editor.fillHeartRect(0, 0, 2, 1, limit = 3)
        assertEquals(3, placed)
        assertEquals(3, editor.heartCount())
        assertEquals("HHH", editor.row(0))
        assertEquals("# #", editor.row(1))
    }

    @Test
    fun `fillHeartRect учитывает уже стоящие сердечки`() {
        val editor = MapEditor(3, 1)
        editor.load("H  \n")
        val placed = editor.fillHeartRect(0, 0, 2, 0, limit = 3)
        assertEquals(2, placed)
        assertEquals("HHH", String(editor.grid[0]))
        // лимит достигнут — повторная заливка ничего не меняет
        assertEquals(0, editor.fillHeartRect(0, 0, 2, 0, limit = 3))
    }

    @Test
    fun `fillHeartRect не ставит на стены и занятые клетки`() {
        val editor = MapEditor(3, 1)
        editor.load("#+1\n")
        assertEquals(0, editor.fillHeartRect(0, 0, 2, 0, limit = 3))
        assertEquals(0, editor.heartCount())
    }

    // ---------- Валидация ----------

    @Test
    fun `validate без старта мыши даёт предупреждение`() {
        val editor = editorOf(
            "#####",
            "# 1 #",
            "#####"
        )
        val v = editor.validate()
        assertFalse(v.hasMouseStart)
        assertTrue(v.warnings().any { it.contains("нет старта мыши") })
    }

    @Test
    fun `validate находит недостижимый сыр`() {
        val editor = editorOf(
            "#######",
            "# 1# 2#",
            "#######"
        )
        val v = editor.validate()
        assertTrue(v.unreachableCheese.contains(GridPoint2(5, 1)), "получено ${v.unreachableCheese}")
        assertTrue(v.warnings().any { it.contains("недостижимый сыр") })
    }

    @Test
    fun `validate без сыра даёт предупреждение`() {
        val editor = editorOf(
            "#####",
            "#+  #",
            "#####"
        )
        val v = editor.validate()
        assertFalse(v.hasCheese)
        assertTrue(v.warnings().any { it.contains("нет сыра") })
    }

    @Test
    fun `validate без стартов котов даёт предупреждение`() {
        val editor = editorOf(
            "#####",
            "#+ 1#",
            "#####"
        )
        val v = editor.validate()
        assertFalse(v.hasAllCats)
        assertTrue(v.warnings().any { it.contains("старты котов") })
    }

    @Test
    fun `validate корректной карты не даёт предупреждений`() {
        val editor = editorOf(
            "#######",
            "#+ 1 #",
            "#A B #",
            "#C D #",
            "#######"
        )
        val v = editor.validate()
        assertTrue(v.isOk, "предупреждения: ${v.warnings()}")
        assertTrue(v.warnings().isEmpty())
        assertEquals(1, v.cheeseCount)
        assertEquals(1, v.mouseStartCount)
        assertEquals(setOf('A', 'B', 'C', 'D'), v.catStarts)
    }

    // ---------- Интеграция с MazeLoader ----------

    @Test
    fun `MazeLoader fromText читает карту редактора`() {
        val maze = MazeLoader.fromText("###\n# #\n###\n")
        assertEquals(3, maze.width)
        assertEquals(3, maze.height)
        assertTrue(maze.isWall(0, 0))
        assertTrue(maze.isPassable(1, 1))
    }
}
