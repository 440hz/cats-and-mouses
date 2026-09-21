package com.catsandmouses

import com.badlogic.gdx.math.GridPoint2
import com.catsandmouses.entities.CatStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Сквозные тесты игрового цикла [GameWorld] (после выноса из GameScreen).
 * Служат эталоном правил игры и защитой от расхождения с Python-портом `tests/check_map.py`.
 */
class GameWorldSimulationTest : GdxTestBase() {

    /** Арена: все клетки стены, внутренняя область проходима; [setup] расставляет объекты. */
    private fun arena(width: Int, height: Int, setup: (Array<CharArray>) -> Unit): MazeLoader {
        val grid = Array(height) { CharArray(width) { '#' } }
        for (r in 1 until height - 1) for (c in 1 until width - 1) grid[r][c] = ' '
        setup(grid)
        return MazeLoader.fromText(grid.joinToString("\n") { String(it) })
    }

    /** Арена с мышью и сыром сверху и с четырьмя котами в изолированной нижней комнате. */
    private fun sealedArena(): MazeLoader = arena(width = 12, height = 7) { g ->
        for (c in 0 until 12) g[4][c] = '#' // разделитель: коты не дойдут до мыши
        g[1][1] = '+'
        g[1][4] = '1'
        g[1][8] = '2'
        g[5][1] = 'A'
        g[5][4] = 'B'
        g[5][7] = 'C'
        g[5][10] = 'D'
    }

    private fun walkUntil(world: GameWorld, dir: Direction, max: Int = 300, predicate: (GameWorld) -> Boolean) {
        var steps = 0
        while (!predicate(world) && steps < max) {
            world.update(1f, dir)
            steps++
        }
        check(steps < max) { "условие не достигнуто за $max шагов" }
    }

    @Test
    fun `старт уровня расставляет мышь сыр и котов`() {
        val maze = sealedArena()
        val world = GameWorld()
        world.startLevel(maze, GridPoint2(1, 1), isFinal = false, keepScore = false)

        assertEquals(GridPoint2(1, 1), world.mouse.cell)
        assertEquals(2, world.pellets.size)
        assertEquals(4, world.cats.size)
        assertEquals(
            listOf(CatStrategy.Chaser, CatStrategy.Patrol, CatStrategy.Random, CatStrategy.Ambush),
            world.cats.map { it.strategy }
        )
        assertEquals(GameWorld.State.PLAYING, world.state)
        assertEquals(0, world.score)
    }

    @Test
    fun `стартовый отсчёт замораживает мышь сыр и котов`() {
        val world = GameWorld()
        world.startLevel(sealedArena(), GridPoint2(1, 1), isFinal = false, keepScore = false)

        assertTrue(world.isCountdown, "после старта уровня идёт отсчёт")
        assertEquals(3, world.countdownNumber)

        val mouseStart = world.mouse.cell
        val catCells = world.cats.map { it.cell.x to it.cell.y }

        world.update(1f, Direction.RIGHT)

        assertEquals(2, world.countdownNumber, "цифра отсчёта убывает 3 → 2")
        assertEquals(mouseStart, world.mouse.cell, "во время отсчёта мышь не двигается")
        assertEquals(catCells, world.cats.map { it.cell.x to it.cell.y }, "во время отсчёта коты не двигаются")
        assertEquals(0, world.pellets.count { it.eaten }, "сыр во время отсчёта не собирается")
    }

    @Test
    fun `после отсчёта игра начинается`() {
        val world = GameWorld()
        world.startLevel(sealedArena(), GridPoint2(1, 1), isFinal = false, keepScore = false)

        world.update(1f, Direction.RIGHT)
        world.update(1f, Direction.RIGHT)
        world.update(1f, Direction.RIGHT)
        assertFalse(world.isCountdown, "отсчёт закончился")

        world.update(1f, Direction.RIGHT) // задать направление и начать движение
        world.update(1f, Direction.RIGHT) // сместиться в следующую клетку
        assertNotEquals(GridPoint2(1, 1), world.mouse.cell, "после отсчёта мышь двигается")
    }

    @Test
    fun `countdown 0 начинает игру сразу`() {
        val world = GameWorld()
        world.startLevel(sealedArena(), GridPoint2(1, 1), isFinal = false, keepScore = false, countdown = 0f)

        assertFalse(world.isCountdown, "при countdown = 0 отсчёта нет")
        world.update(1f, Direction.RIGHT) // задать направление и начать движение
        world.update(1f, Direction.RIGHT) // сместиться в следующую клетку
        assertNotEquals(GridPoint2(1, 1), world.mouse.cell, "игра идёт сразу")
    }

    @Test
    fun `старт уровня расставляет сердечки по символу H`() {
        val maze = arena(width = 12, height = 7) { g ->
            for (c in 0 until 12) g[4][c] = '#'
            g[1][1] = '+'
            g[1][4] = 'H'
            g[1][8] = '1'
            g[5][1] = 'A'
            g[5][4] = 'B'
            g[5][7] = 'C'
            g[5][10] = 'D'
        }
        val world = GameWorld()
        world.startLevel(maze, GridPoint2(1, 1), isFinal = false, keepScore = false)

        assertEquals(1, world.hearts.size)
        assertEquals(GridPoint2(4, 1), world.hearts[0].cell)
        assertTrue(world.hearts.none { it.collected })
    }

    @Test
    fun `сердечко даёт жизнь без очков`() {
        val maze = arena(width = 12, height = 7) { g ->
            for (c in 0 until 12) g[4][c] = '#'
            g[1][1] = '+'
            g[1][4] = 'H'
            g[1][8] = '1'
            g[5][1] = 'A'
            g[5][4] = 'B'
            g[5][7] = 'C'
            g[5][10] = 'D'
        }
        val world = GameWorld()
        world.startLevel(maze, GridPoint2(1, 1), isFinal = false, keepScore = false)

        world.update(1f, Direction.RIGHT) // старт движения
        walkUntil(world, Direction.RIGHT) { it.hearts[0].collected }

        assertTrue(world.hearts[0].collected)
        assertEquals(Constants.START_LIVES + 1, world.lives, "сердечко даёт +1 жизнь")
        assertEquals(GameWorld.State.PLAYING, world.state)
        assertEquals(0, world.score, "сердечко не даёт очков")
        assertEquals(0, world.pellets.count { it.eaten })
    }

    @Test
    fun `жизни не превышают максимум при подборе сердечек`() {
        val world = GameWorld()
        world.startLevel(sealedHeartArena(), GridPoint2(1, 1), isFinal = false, keepScore = false, startLives = Constants.MAX_LIVES)

        world.update(1f, Direction.RIGHT)
        walkUntil(world, Direction.RIGHT) { it.hearts[0].collected }

        assertEquals(Constants.MAX_LIVES, world.lives, "выше MAX_LIVES не растёт")
    }

    @Test
    fun `старт уровня задаёт жизни с ограничением снизу`() {
        val maze = sealedArena()
        val world = GameWorld()

        world.startLevel(maze, GridPoint2(1, 1), isFinal = false, keepScore = false, startLives = 5)
        assertEquals(5, world.lives)

        world.startLevel(maze, GridPoint2(1, 1), isFinal = false, keepScore = false, startLives = 0)
        assertEquals(Constants.START_LIVES, world.lives, "ниже START_LIVES не опускается")
    }

    @Test
    fun `поимка с запасом жизни тратит жизнь телепортирует и даёт неуязвимость`() {
        val world = GameWorld()
        world.startLevel(adjacentCatArena(), GridPoint2(1, 1), isFinal = false, keepScore = false, startLives = 2, countdown = 0f)

        world.update(0.3f, Direction.RIGHT) // старт движения
        world.update(0.3f, Direction.RIGHT) // мышь входит в клетку кота A

        assertEquals(GameWorld.State.PLAYING, world.state)
        assertEquals(1, world.lives)
        assertTrue(world.isInvulnerable, "после потери жизни мышь неуязвима")
        assertNotEquals(GridPoint2(2, 1), world.mouse.cell, "мышь должна телепортироваться")
        assertEquals(1, world.catchCount["Барсик"])
        assertEquals(0, world.score)
    }

    @Test
    fun `в неуязвимости поимка игнорируется`() {
        val world = GameWorld()
        world.startLevel(adjacentCatArena(), GridPoint2(1, 1), isFinal = false, keepScore = false, startLives = 2, countdown = 0f)

        world.update(0.3f, Direction.RIGHT) // старт движения
        world.update(0.3f, Direction.RIGHT) // первая поимка → телепорт + неуязвимость
        assertTrue(world.isInvulnerable)

        val catCell = world.cats[0].cell
        world.mouse.teleportTo(catCell.x, catCell.y)
        world.update(0.1f, Direction.NONE)

        assertEquals(GameWorld.State.PLAYING, world.state)
        assertEquals(1, world.lives)
    }

    @Test
    fun `после неуязвимости поимка на последней жизни завершает игру`() {
        val world = GameWorld()
        world.startLevel(adjacentCatArena(), GridPoint2(1, 1), isFinal = false, keepScore = false, startLives = 2, countdown = 0f)

        world.update(0.3f, Direction.RIGHT) // старт движения
        world.update(0.3f, Direction.RIGHT) // первая поимка → lives=1, неуязвимость
        assertEquals(1, world.lives)

        val catCell = world.cats[0].cell
        world.mouse.teleportTo(catCell.x, catCell.y)
        world.update(Constants.INVULN_TIME + 0.5f, Direction.NONE) // неуязвимость истекла

        assertEquals(GameWorld.State.GAME_OVER, world.state)
        assertEquals(0, world.lives)
    }

    private fun sealedHeartArena(): MazeLoader = arena(width = 12, height = 7) { g ->
        for (c in 0 until 12) g[4][c] = '#'
        g[1][1] = '+'
        g[1][4] = 'H'
        g[1][8] = '1'
        g[5][1] = 'A'
        g[5][4] = 'B'
        g[5][7] = 'C'
        g[5][10] = 'D'
    }

    /** Арена, где кот A стоит вплотную к старту мыши (для проверок поимки). */
    private fun adjacentCatArena(): MazeLoader = arena(width = 12, height = 7) { g ->
        g[1][1] = '+'
        g[1][2] = 'A'
        g[1][8] = '1'
        g[5][1] = 'B'
        g[5][4] = 'C'
        g[5][7] = 'D'
    }

    @Test
    fun `финальный уровень включает четырёх преследователей`() {
        val maze = sealedArena()
        val world = GameWorld()
        world.startLevel(maze, GridPoint2(1, 1), isFinal = true, keepScore = false)

        assertEquals(List(4) { CatStrategy.Chaser }, world.cats.map { it.strategy })
    }

    @Test
    fun `сбор всего сыра даёт победу и счёт`() {
        val world = GameWorld()
        world.startLevel(sealedArena(), GridPoint2(1, 1), isFinal = false, keepScore = false)

        walkUntil(world, Direction.RIGHT) { it.score == 100 }
        assertEquals(1, world.pellets.count { it.eaten })
        assertEquals(GameWorld.State.PLAYING, world.state)

        walkUntil(world, Direction.RIGHT) { it.state == GameWorld.State.WIN }
        assertEquals(200, world.score)
        assertTrue(world.pellets.all { it.eaten })
    }

    @Test
    fun `максимум очков не превышает число съеденных сыров`() {
        val world = GameWorld()
        world.startLevel(sealedArena(), GridPoint2(1, 1), isFinal = false, keepScore = false)

        walkUntil(world, Direction.RIGHT) { it.score == 100 }
        walkUntil(world, Direction.RIGHT) { it.mouse.cell.x == 6 }
        assertEquals(100, world.score)

        // Возвращаемся на уже съеденную клетку (4,1)
        walkUntil(world, Direction.LEFT) { it.mouse.cell.x == 4 }
        assertEquals(100, world.score, "повторный проход по сыру не добавляет очки")
        assertEquals(1, world.pellets.count { it.eaten })

        walkUntil(world, Direction.RIGHT) { it.state == GameWorld.State.WIN }
        assertEquals(200, world.score)
    }

    @Test
    fun `столкновение с котом завершает игру`() {
        val maze = arena(width = 10, height = 6) { g ->
            g[1][1] = '+'
            g[1][2] = 'A'
            g[1][5] = '1'
            g[3][2] = 'B'
            g[3][4] = 'C'
            g[3][7] = 'D'
        }
        val world = GameWorld()
        world.startLevel(maze, GridPoint2(1, 1), isFinal = false, keepScore = false, countdown = 0f)

        world.update(0.3f, Direction.RIGHT) // мышь начинает движение к коту
        assertEquals(GameWorld.State.PLAYING, world.state)

        world.update(0.3f, Direction.RIGHT) // мышь входит в клетку кота
        assertEquals(GameWorld.State.GAME_OVER, world.state)
        assertEquals("Барсик", world.lastCatcher?.name)
        assertEquals(1, world.catchCount["Барсик"])
        assertEquals(0, world.score)
    }

    @Test
    fun `keepScore сохраняет счёт при перезапуске`() {
        val maze = sealedArena()
        val world = GameWorld()
        world.startLevel(maze, GridPoint2(1, 1), isFinal = false, keepScore = false)
        world.setScore(300)

        world.startLevel(maze, GridPoint2(1, 1), isFinal = false, keepScore = true)
        assertEquals(300, world.score)

        world.startLevel(maze, GridPoint2(1, 1), isFinal = false, keepScore = false)
        assertEquals(0, world.score)
    }

    @Test
    fun `перезапуск уровня сбрасывает состояние игры`() {
        val maze = sealedArena()
        val world = GameWorld()
        world.startLevel(maze, GridPoint2(1, 1), isFinal = false, keepScore = false)
        world.update(0.3f, Direction.RIGHT)

        // Искусственно доводим до победы, затем перезапускаем
        walkUntil(world, Direction.RIGHT) { it.state == GameWorld.State.WIN }
        assertEquals(GameWorld.State.WIN, world.state)

        world.startLevel(maze, GridPoint2(1, 1), isFinal = false, keepScore = false)
        assertEquals(GameWorld.State.PLAYING, world.state)
        assertTrue(world.pellets.none { it.eaten })
    }
}
