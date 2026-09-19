package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.GridPoint2
import com.catsandmouses.entities.Cat
import com.catsandmouses.entities.CatStrategy
import com.catsandmouses.entities.Mouse
import com.catsandmouses.entities.Pellet
import kotlin.random.Random

/**
 * Игровая симуляция без рендера: лабиринт, мышь, коты, сыр, счёт и состояние.
 * GameScreen отвечает за темы, спрайты, HUD и оверлеи, а кадровую логику делегирует сюда —
 * благодаря этому правила игры тестируются headless и сверяются с `tests/check_map.py`.
 *
 * Порядок кадра в [update] повторяет прежний `GameScreen.updatePlaying`:
 * мышь → коллизия до котов → поедание сыра → обновление котов с резервацией → коллизия → победа.
 */
class GameWorld {

    enum class State { PLAYING, GAME_OVER, WIN }

    lateinit var maze: MazeLoader
        private set

    lateinit var mouse: Mouse
        private set

    val cats = mutableListOf<Cat>()
    val pellets = mutableListOf<Pellet>()

    var score = 0
        private set

    var state = State.PLAYING
        private set

    var lastCatcher: Cat? = null
        private set

    val catchCount = mutableMapOf<String, Int>()

    /** Установить счёт напрямую (стартовый счёт из меню/отладки). */
    fun setScore(value: Int) {
        score = value
    }

    /**
     * Расставить мышь, котов и сыр. Позиции котов берутся из букв A..D, иначе —
     * случайные достижимые клетки у углов (как в GameScreen).
     */
    fun startLevel(maze: MazeLoader, mouseStart: GridPoint2, isFinal: Boolean, keepScore: Boolean) {
        this.maze = maze
        mouse = Mouse(maze, mouseStart.x, mouseStart.y)
        cats.clear()

        val reachable = maze.findReachableCells(mouse.cell.x, mouse.cell.y).toSet()
        val explicitCatStarts = maze.findCatStarts()
        val catStarts = if (explicitCatStarts.size == 4) {
            listOf(
                explicitCatStarts.getValue('A'),
                explicitCatStarts.getValue('B'),
                explicitCatStarts.getValue('C'),
                explicitCatStarts.getValue('D')
            )
        } else {
            listOf(
                randomizeStart(1, 1, 1, 1, reachable),
                randomizeStart(Constants.MAZE_COLS - 2, 1, -1, 1, reachable),
                randomizeStart(1, Constants.MAZE_ROWS - 3, 1, -1, reachable),
                randomizeStart(Constants.MAZE_COLS - 2, Constants.MAZE_ROWS - 3, -1, -1, reachable)
            )
        }
        val strategies = if (isFinal) {
            listOf(CatStrategy.Chaser, CatStrategy.Chaser, CatStrategy.Chaser, CatStrategy.Chaser)
        } else {
            listOf(CatStrategy.Chaser, CatStrategy.Patrol, CatStrategy.Random, CatStrategy.Ambush)
        }
        val colors = listOf(
            Color(1f, 0.125f, 0.125f, 1f),
            Color(0.125f, 0.375f, 1f, 1f),
            Color(0.625f, 0.125f, 0.94f, 1f),
            Color(0.08f, 0.08f, 0.08f, 1f)
        )
        val names = listOf("Барсик", "Матроскин", "Леопольд", "Черныш")
        val speeds = if (isFinal) List(4) { 150f } else List(4) { 120f }

        for (i in 0..3) {
            cats.add(Cat(maze, catStarts[i].x, catStarts[i].y, strategies[i], colors[i], names[i], speeds[i]))
        }

        pellets.clear()
        spawnPellets(explicitCatStarts)
        if (!keepScore) score = 0
        lastCatcher = null
        state = State.PLAYING
    }

    /** Один кадр игры. [inputDirection] — направление мыши из ввода/AI. */
    fun update(delta: Float, inputDirection: Direction) {
        mouse.setNextDirection(inputDirection)
        mouse.update(delta)

        if (checkCatCollision()) return

        for (pellet in pellets) {
            if (!pellet.eaten && pellet.cell == mouse.cell) {
                pellet.eat()
                score += 100
            }
        }

        val reservedCells = mutableSetOf<Pair<Int, Int>>()
        cats.forEach { reservedCells.add(it.cell.x to it.cell.y) }

        for (cat in cats) {
            cat.update(delta, mouse.cell, mouse.direction, reservedCells)
            reservedCells.add(cat.cell.x to cat.cell.y)
        }

        if (checkCatCollision()) return

        if (pellets.all { it.eaten }) {
            state = State.WIN
            Gdx.app.log("GameWorld", "WIN, score=$score")
        }
    }

    private fun checkCatCollision(): Boolean {
        for (cat in cats) {
            if (cat.cell == mouse.cell) {
                state = State.GAME_OVER
                Gdx.app.log("GameWorld", "GAME_OVER, catcher=${cat.name}")
                lastCatcher = cat
                catchCount[cat.name] = catchCount.getOrDefault(cat.name, 0) + 1
                return true
            }
        }
        return false
    }

    private fun randomizeStart(
        cornerCol: Int,
        cornerRow: Int,
        signCol: Int,
        signRow: Int,
        reachable: Set<GridPoint2>
    ): GridPoint2 {
        val candidates = mutableListOf<GridPoint2>()
        for (dc in 0..4) {
            for (dr in 0..4) {
                val col = cornerCol + dc * signCol
                val row = cornerRow + dr * signRow
                val cell = GridPoint2(col, row)
                if (reachable.contains(cell)) {
                    candidates.add(cell)
                }
            }
        }
        if (candidates.isNotEmpty()) return candidates.random()

        return reachable.minByOrNull {
            kotlin.math.abs(it.x - cornerCol) + kotlin.math.abs(it.y - cornerRow)
        } ?: GridPoint2(cornerCol, cornerRow)
    }

    private fun spawnPellets(catStarts: Map<Char, GridPoint2>) {
        val reachable = maze.findReachableCells(mouse.cell.x, mouse.cell.y).toSet()
        val catStartCells = catStarts.values.toSet()

        val occupied = mutableSetOf<GridPoint2>()
        occupied.add(GridPoint2(mouse.cell.x, mouse.cell.y))
        occupied.addAll(catStartCells)
        cats.forEach { occupied.add(GridPoint2(it.cell.x, it.cell.y)) }

        val manualCells = maze.findCheeseCells()
        val pelletCells = mutableListOf<GridPoint2>()

        if (manualCells.isNotEmpty()) {
            var replaced = 0
            for (cell in manualCells) {
                if (!reachable.contains(cell)) {
                    replaced++
                } else if (occupied.contains(cell)) {
                    replaced++
                } else {
                    pelletCells.add(cell)
                }
            }

            val used = (occupied + pelletCells.toSet()).toMutableSet()
            val available = reachable.filter { !used.contains(it) }.shuffled()
            val fillCount = minOf(replaced, available.size)
            for (i in 0 until fillCount) {
                pelletCells.add(available[i])
            }
        } else if (catStartCells.isNotEmpty()) {
            for (cell in catStartCells) {
                if (reachable.contains(cell)) {
                    pelletCells.add(cell)
                }
            }
        } else {
            val available = reachable.filter { !occupied.contains(it) }.shuffled()
            val count = minOf(3, available.size)
            for (i in 0 until count) {
                pelletCells.add(available[i])
            }
        }

        pellets.clear()
        for (cell in pelletCells) {
            pellets.add(Pellet(cell, maze))
        }
    }
}
