package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.GridPoint2
import com.catsandmouses.entities.Cat
import com.catsandmouses.entities.CatStrategy
import com.catsandmouses.entities.Heart
import com.catsandmouses.entities.Mouse
import com.catsandmouses.entities.Pellet
import com.catsandmouses.i18n.I18n
import com.catsandmouses.i18n.Str
import kotlin.random.Random

/**
 * Игровая симуляция без рендера: лабиринт, мышь, коты, сыр, счёт и состояние.
 * GameScreen отвечает за темы, спрайты, HUD и оверлеи, а кадровую логику делегирует сюда —
 * благодаря этому правила игры тестируются headless и сверяются с `tests/check_map.py`.
 *
 * Порядок кадра в [update] повторяет прежний `GameScreen.updatePlaying`:
 * мышь → коллизия до котов → поедание сыра → обновление котов с резервацией → коллизия → победа.
 *
 * Финал (`isFinal`): победа инвертирована — уровень проходится, только если мышь **ни разу
 * не сдвинулась**: поимка такой мыши котом ([checkCatCollision]) даёт [State.WIN]. Если мышь
 * хоть раз двигалась — поимка обрабатывается как обычная потеря жизни/проигрыш. Сыр на финале
 * победы не даёт (`pellets.all { eaten }` игнорируется).
 */
class GameWorld {

    enum class State { PLAYING, GAME_OVER, WIN }

    lateinit var maze: MazeLoader
        private set

    lateinit var mouse: Mouse
        private set

    val cats = mutableListOf<Cat>()
    val pellets = mutableListOf<Pellet>()
    val hearts = mutableListOf<Heart>()

    var score = 0
        private set

    /** Текущий запас жизней мыши (растёт от сердечек, тратится на поимках). */
    var lives = Constants.START_LIVES
        private set

    /** Остаток неуязвимости после потери жизни, сек. */
    var invulnTimer = 0f
        private set

    val isInvulnerable: Boolean get() = invulnTimer > 0f

    /** Остаток стартового отсчёта «3 → 2 → 1», сек. Пока > 0 — игра заморожена. */
    var countdownTimer = 0f
        private set

    val isCountdown: Boolean get() = countdownTimer > 0f

    /** Текущая цифра отсчёта (3, 2 или 1). Вне отсчёта не определена. */
    val countdownNumber: Int get() = kotlin.math.ceil(countdownTimer).toInt().coerceAtLeast(1)

    var state = State.PLAYING
        private set

    var lastCatcher: Cat? = null
        private set

    /** Финал: победа — быть пойманным котом, ни разу не сдвинувшись с места. */
    private var finalLevel = false

    /** Двигалась ли мышь хоть раз за текущий уровень (правило финала). */
    var mouseEverMoved = false
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
    fun startLevel(
        maze: MazeLoader,
        mouseStart: GridPoint2,
        isFinal: Boolean,
        keepScore: Boolean,
        startLives: Int = Constants.START_LIVES,
        /** Длительность стартового отсчёта; 0 — игра начинается сразу. */
        countdown: Float = Constants.COUNTDOWN_TIME,
        /** Куда смотрит мышь на старте (только поворот спрайта). */
        mouseFacing: Direction = Direction.NONE
    ) {
        this.maze = maze
        val mouseSpeed = if (isFinal) Constants.MOUSE_SPEED_FINAL else Constants.MOUSE_SPEED
        mouse = Mouse(maze, mouseStart.x, mouseStart.y, mouseSpeed)
        mouse.setFacing(mouseFacing)
        cats.clear()

        val reachable = maze.findReachableCells(mouse.cell.x, mouse.cell.y).toSet()
        val explicitCatStarts = maze.findCatStarts()
        // Старты котов: явные буквы A..D из карты (1–4 шт.), иначе — 4 случайных у углов
        val catStarts: List<GridPoint2> = if (explicitCatStarts.isNotEmpty()) {
            ('A'..'D').mapNotNull { explicitCatStarts[it] }
        } else {
            listOf(
                randomizeStart(1, 1, 1, 1, reachable),
                randomizeStart(Constants.MAZE_COLS - 2, 1, -1, 1, reachable),
                randomizeStart(1, Constants.MAZE_ROWS - 3, 1, -1, reachable),
                randomizeStart(Constants.MAZE_COLS - 2, Constants.MAZE_ROWS - 3, -1, -1, reachable)
            )
        }
        val catCount = catStarts.size
        val strategies = if (isFinal) {
            List(catCount) { CatStrategy.Chaser }
        } else {
            listOf(CatStrategy.Chaser, CatStrategy.Patrol, CatStrategy.Random, CatStrategy.Ambush).take(catCount)
        }
        val colors = listOf(
            Color(1f, 0.125f, 0.125f, 1f),
            Color(0.125f, 0.375f, 1f, 1f),
            Color(0.625f, 0.125f, 0.94f, 1f),
            Color(0.08f, 0.08f, 0.08f, 1f)
        ).take(catCount)
        // Имена котов по ролям (Chaser / Patrol / Random / Ambush) — локализованы (RU/EN)
        val names = listOf(
            I18n.t(Str.CAT_CHASER),
            I18n.t(Str.CAT_PATROL),
            I18n.t(Str.CAT_RANDOM),
            I18n.t(Str.CAT_AMBUSH)
        ).take(catCount)
        // Финал: кот быстрее мыши (CAT_SPEED_FINAL > MOUSE_SPEED); обычные уровни — медленнее
        val speeds = if (isFinal) {
            List(catCount) { Constants.CAT_SPEED_FINAL }
        } else {
            List(catCount) { Constants.CAT_SPEED }
        }

        for (i in 0 until catCount) {
            cats.add(Cat(maze, catStarts[i].x, catStarts[i].y, strategies[i], colors[i], names[i], speeds[i]))
        }

        pellets.clear()
        spawnPellets(explicitCatStarts)
        spawnHearts()
        if (!keepScore) score = 0
        lives = startLives.coerceIn(Constants.START_LIVES, Constants.MAX_LIVES)
        invulnTimer = 0f
        countdownTimer = countdown.coerceAtLeast(0f)
        lastCatcher = null
        finalLevel = isFinal
        mouseEverMoved = false
        state = State.PLAYING
    }

    /** Один кадр игры. [inputDirection] — направление мыши из ввода/AI. */
    fun update(delta: Float, inputDirection: Direction) {
        // Стартовый отсчёт: мышь и коты заморожены, ввод буферизуется InputHandler'ом
        if (countdownTimer > 0f) {
            countdownTimer = (countdownTimer - delta).coerceAtLeast(0f)
            return
        }

        if (invulnTimer > 0f) {
            invulnTimer = (invulnTimer - delta).coerceAtLeast(0f)
        }

        mouse.setNextDirection(inputDirection)
        mouse.update(delta)
        // Финал: любое движение мыши отменяет «победную» поимку
        if (mouse.direction != Direction.NONE) mouseEverMoved = true

        if (checkCatCollision()) return

        for (pellet in pellets) {
            if (!pellet.eaten && pellet.cell == mouse.cell) {
                pellet.eat()
                score += 100
            }
        }

        // Сердечки: +1 жизнь до MAX_LIVES (этап 15.3)
        for (heart in hearts) {
            if (!heart.collected && heart.cell == mouse.cell) {
                heart.collect()
                lives = (lives + 1).coerceAtMost(Constants.MAX_LIVES)
            }
        }

        val reservedCells = mutableSetOf<Pair<Int, Int>>()
        cats.forEach { reservedCells.add(it.cell.x to it.cell.y) }

        for (cat in cats) {
            cat.update(delta, mouse.cell, mouse.direction, reservedCells)
            reservedCells.add(cat.cell.x to cat.cell.y)
        }

        if (checkCatCollision()) return

        // На финале победа не по сыру — только по «победной» поимке (см. checkCatCollision)
        if (!finalLevel && pellets.all { it.eaten }) {
            state = State.WIN
            Gdx.app.log("GameWorld", "WIN, score=$score")
        }
    }

    /**
     * Поимка мыши котом. В неуязвимости — игнорируется. Иначе тратится жизнь:
     * при запасе жизни мышь телепортируется в самую далёкую от котов клетку и
     * получает неуязвимость; на последней жизни — [State.GAME_OVER].
     */
    private fun checkCatCollision(): Boolean {
        if (isInvulnerable) return false

        val catcher = cats.firstOrNull { it.cell == mouse.cell } ?: return false
        lastCatcher = catcher

        // Финал: поимка мыши, которая ни разу не двигалась, — это ПОБЕДА
        if (finalLevel && !mouseEverMoved) {
            state = State.WIN
            Gdx.app.log("GameWorld", "FINAL WIN: caught while standing still, catcher=${catcher.name}")
            return true
        }

        catchCount[catcher.name] = catchCount.getOrDefault(catcher.name, 0) + 1

        if (lives > 1) {
            lives--
            val safe = farthestCellFromCats()
            mouse.teleportTo(safe.x, safe.y)
            invulnTimer = Constants.INVULN_TIME
            Gdx.app.log("GameWorld", "CAUGHT by ${catcher.name}, lives=$lives, teleport=${safe.x},${safe.y}")
            return false
        }

        lives = 0
        state = State.GAME_OVER
        Gdx.app.log("GameWorld", "GAME_OVER, catcher=${catcher.name}")
        return true
    }

    /** Достижимая клетка, максимально далёкая от всех котов (BFS от котов). */
    private fun farthestCellFromCats(): GridPoint2 {
        val reachable = maze.findReachableCells(mouse.cell.x, mouse.cell.y)
        if (reachable.isEmpty()) return mouse.cell

        val distance = mutableMapOf<GridPoint2, Int>()
        val queue = ArrayDeque<GridPoint2>()
        for (cat in cats) {
            val cell = GridPoint2(cat.cell.x, cat.cell.y)
            if (maze.isPassable(cell.x, cell.y) && !distance.containsKey(cell)) {
                distance[cell] = 0
                queue.add(cell)
            }
        }
        val dirs = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val d = distance.getValue(current)
            for (dir in dirs) {
                val next = GridPoint2(current.x + dir.deltaCol, current.y + dir.deltaRow)
                if (maze.isPassable(next.x, next.y) && !distance.containsKey(next)) {
                    distance[next] = d + 1
                    queue.add(next)
                }
            }
        }

        val catCells = cats.map { it.cell.x to it.cell.y }.toSet()
        return reachable
            .filter { (it.x to it.y) !in catCells }
            .maxWithOrNull(compareBy({ distance[it] ?: -1 }, { it.x }, { it.y }))
            ?: mouse.cell
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

    /** Расставить сердечки по символам `H` (до 3 на карту задаётся редактором). */
    private fun spawnHearts() {
        hearts.clear()
        for (cell in maze.findHeartCells()) {
            hearts.add(Heart(cell, maze))
        }
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
