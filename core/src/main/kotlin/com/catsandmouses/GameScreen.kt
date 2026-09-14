package com.catsandmouses

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.utils.viewport.FitViewport
import kotlin.random.Random
import com.catsandmouses.entities.Cat
import com.catsandmouses.entities.CatStrategy
import com.catsandmouses.entities.Mouse
import com.catsandmouses.entities.Pellet

class GameScreen(
    private val debugStartMap: String = "1/1",
    private val debugLogs: Boolean = false
) : Screen {

    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var font: BitmapFont
    private var backgroundTexture: Texture? = null
    private val wallTextures = mutableListOf<Texture>()
    private var mouseTexture: Texture? = null
    private var cheeseTexture: Texture? = null
    private var catChaserTexture: Texture? = null
    private var catPatrolTexture: Texture? = null
    private var catRandomTexture: Texture? = null
    private var catAmbushTexture: Texture? = null
    private lateinit var backgroundRegion: TextureRegion
    private val wallRegions = mutableListOf<TextureRegion>()
    private lateinit var mouseRegion: TextureRegion
    private lateinit var cheeseRegion: TextureRegion
    private lateinit var catChaserRegion: TextureRegion
    private lateinit var catPatrolRegion: TextureRegion
    private lateinit var catRandomRegion: TextureRegion
    private lateinit var catAmbushRegion: TextureRegion
    private lateinit var inputHandler: InputHandler
    private lateinit var theme: ThemeLoader
    private lateinit var maze: MazeLoader
    private lateinit var mouse: Mouse
    private val cats = mutableListOf<Cat>()
    private val pellets = mutableListOf<Pellet>()
    // Стенные клетки, где вместо основного спрайта рисуется случайный доп темы
    private val extraWalls = mutableMapOf<GridPoint2, TextureRegion>()

    private var score = 0
    private var state = GameState.PLAYING
    private var stateTimer = 0f
    private var lastCatcher: Cat? = null
    private val catchCount = mutableMapOf<String, Int>()
    private var isPaused = false

    private val mapFiles = listOf(
        "maps/1/1.txt",
        "maps/1/2.txt",
        "maps/1/3.txt",
        "maps/1/4.txt",
        "maps/final.txt"
    )
    private var currentLevel = mapFiles.indexOf("maps/$debugStartMap.txt").coerceAtLeast(0)

    override fun show() {
        // Уровень логов: по умолчанию отключены, включаются флагом --debug
        Gdx.app.logLevel = if (debugLogs) Application.LOG_DEBUG else Application.LOG_NONE

        camera = OrthographicCamera(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)
        camera.position.set(Constants.WORLD_WIDTH / 2f, Constants.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        viewport = FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, camera)
        shapeRenderer = ShapeRenderer()
        shapeRenderer.projectionMatrix = camera.combined
        spriteBatch = SpriteBatch()
        spriteBatch.projectionMatrix = camera.combined
        font = generateFont()
        inputHandler = InputHandler()
        Gdx.input.inputProcessor = inputHandler

        // Текстуры активной темы: фон и стены (пиксель-арт, без сглаживания)
        theme = ThemeLoader()
        backgroundTexture = Texture(Gdx.files.internal(theme.backgroundPath))
        wallTextures.clear()
        for (path in theme.wallPaths) {
            val texture = Texture(Gdx.files.internal(path))
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            wallTextures.add(texture)
        }
        mouseTexture = Texture(Gdx.files.internal("sprites/mouse.png"))
        cheeseTexture = Texture(Gdx.files.internal("sprites/cheese.png"))
        catChaserTexture = Texture(Gdx.files.internal("sprites/cat_chaser.png"))
        catPatrolTexture = Texture(Gdx.files.internal("sprites/cat_patrol.png"))
        catRandomTexture = Texture(Gdx.files.internal("sprites/cat_random.png"))
        catAmbushTexture = Texture(Gdx.files.internal("sprites/cat_ambush.png"))
        backgroundTexture!!.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        mouseTexture!!.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        cheeseTexture!!.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        catChaserTexture!!.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        catPatrolTexture!!.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        catRandomTexture!!.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        catAmbushTexture!!.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        backgroundRegion = TextureRegion(backgroundTexture)
        wallRegions.clear()
        for (texture in wallTextures) {
            wallRegions.add(TextureRegion(texture))
        }
        mouseRegion = TextureRegion(mouseTexture)
        cheeseRegion = TextureRegion(cheeseTexture)
        catChaserRegion = TextureRegion(catChaserTexture)
        catPatrolRegion = TextureRegion(catPatrolTexture)
        catRandomRegion = TextureRegion(catRandomTexture)
        catAmbushRegion = TextureRegion(catAmbushTexture)

        startNewGame()
    }

    private fun generateFont(): BitmapFont {
        val fontFile = Gdx.files.internal("fonts/ptsans.ttf")
        val generator = FreeTypeFontGenerator(fontFile)
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 20
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS +
            "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюя" +
            "0123456789:/ "
        val generatedFont = generator.generateFont(parameter)
        generator.dispose()
        return generatedFont
    }

    private fun startNewGame(keepScore: Boolean = false) {
        inputHandler.reset()
        Gdx.app.log("Game", "Loading map ${currentLevel + 1}/${mapFiles.size}: ${mapFiles[currentLevel]}")
        maze = MazeLoader(mapFiles[currentLevel])
        setupExtraWalls()
        val mouseStart = maze.findMouseStart()
        mouse = Mouse(maze, mouseStart.x, mouseStart.y)
        cats.clear()

        val isFinal = currentLevel == mapFiles.size - 1
        val reachable = maze.findReachableCells(mouse.cell.x, mouse.cell.y).toSet()
        val explicitCatStarts = maze.findCatStarts()
        val catStarts = if (explicitCatStarts.size == 4) {
            // Карта задаёт стартовые клетки котов буквами A..D
            listOf(
                explicitCatStarts.getValue('A'),
                explicitCatStarts.getValue('B'),
                explicitCatStarts.getValue('C'),
                explicitCatStarts.getValue('D')
            )
        } else {
            listOf(
                randomizeStart(1, 1, 1, 1, reachable),
                randomizeStart(26, 1, -1, 1, reachable),
                randomizeStart(1, 13, 1, -1, reachable),
                randomizeStart(26, 13, -1, -1, reachable)
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
        state = GameState.PLAYING
        stateTimer = 0f
        isPaused = false
    }

    /**
     * Стартовая позиция кота: случайная клетка в радиусе до 4 клеток от угла
     * в сторону центра карты. Обязательно должна быть достижима для мыши.
     */
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

        // Fallback: ближайшая к углу достижимая клетка (если сам угол недостижим/стена)
        return reachable.minByOrNull {
            kotlin.math.abs(it.x - cornerCol) + kotlin.math.abs(it.y - cornerRow)
        } ?: GridPoint2(cornerCol, cornerRow)
    }

    private fun spawnPellets(catStarts: Map<Char, GridPoint2>) {
        val reachable = maze.findReachableCells(mouse.cell.x, mouse.cell.y).toSet()
        val catStartCells = catStarts.values.toSet()

        val occupied = mutableSetOf<GridPoint2>()
        occupied.add(GridPoint2(mouse.cell.x, mouse.cell.y)) // старт мыши
        occupied.addAll(catStartCells) // стартовые клетки котов не занимают сыр
        cats.forEach { occupied.add(GridPoint2(it.cell.x, it.cell.y)) }

        val manualCells = maze.findCheeseCells()
        val pelletCells = mutableListOf<GridPoint2>()

        if (manualCells.isNotEmpty()) {
            // Ручная расстановка: цифры 1..9 = сыр
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

            // Автозамена недостижимых/занятых ручных клеток
            val used = (occupied + pelletCells.toSet()).toMutableSet()
            val available = reachable.filter { !used.contains(it) }.shuffled()
            val fillCount = minOf(replaced, available.size)
            for (i in 0 until fillCount) {
                pelletCells.add(available[i])
            }
        } else if (catStartCells.isNotEmpty()) {
            // Если цифр сыра нет, но заданы старты котов A..D —
            // сыр появляется там, откуда стартуют коты
            for (cell in catStartCells) {
                if (reachable.contains(cell) && !occupied.contains(cell)) {
                    pelletCells.add(cell)
                }
            }
        } else {
            // Автогенерация fallback: 3 случайные достижимые клетки
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

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        when (state) {
            GameState.PLAYING -> {
                if (inputHandler.isPauseJustPressed()) {
                    isPaused = !isPaused
                }
                if (!isPaused) {
                    updatePlaying(delta)
                }
            }
            GameState.GAME_OVER -> updateGameOver(delta)
            GameState.WIN -> updateWin(delta)
        }

        // 1) Фон и стены через SpriteBatch
        spriteBatch.projectionMatrix = camera.combined
        drawBackgroundAndWalls()

        // 2) Сыр, мышь и коты
        drawPellets()
        drawEntities()

        drawHud()

        if (state == GameState.GAME_OVER) {
            val catcher = lastCatcher
            if (catcher != null) {
                drawCenteredText("Поймал ${catcher.name}", catcher.color)
            } else {
                drawCenteredText("GAME OVER", Color.RED)
            }
        } else if (state == GameState.WIN) {
            drawCenteredText("YOU WIN", Color.GREEN)
        }
    }

    private fun updatePlaying(delta: Float) {
        // a) Обновляем мышь
        mouse.setNextDirection(inputHandler.getDirection())
        mouse.update(delta)

        // b) Проверяем коллизию до обновления котов
        if (checkCatCollision()) return

        // Едим точки
        for (pellet in pellets) {
            if (!pellet.eaten && pellet.cell == mouse.cell) {
                pellet.eat()
                score += 100
            }
        }

        // c) Резервируем только клетки котов (клетка мыши НЕ бронируется)
        val reservedCells = mutableSetOf<Pair<Int, Int>>()
        cats.forEach { reservedCells.add(it.cell.x to it.cell.y) }

        // e) Обновляем котов последовательно
        for (cat in cats) {
            cat.update(delta, mouse.cell, mouse.direction, reservedCells)
            reservedCells.add(cat.cell.x to cat.cell.y)
        }

        // f) Проверяем коллизию после обновления котов
        if (checkCatCollision()) return

        // Проверяем победу
        if (pellets.all { it.eaten }) {
            state = GameState.WIN
            stateTimer = 0f
        }
    }

    private fun checkCatCollision(): Boolean {
        for (cat in cats) {
            if (cat.cell == mouse.cell) {
                state = GameState.GAME_OVER
                stateTimer = 0f
                lastCatcher = cat
                catchCount[cat.name] = catchCount.getOrDefault(cat.name, 0) + 1
                return true
            }
        }
        return false
    }

    private fun updateGameOver(delta: Float) {
        stateTimer += delta
        if (stateTimer >= 2f) {
            // В обычном режиме рестарт с карты №1; в отладочном — остаёмся на текущей
            if (debugStartMap == "1/1") {
                currentLevel = 0
            }
            startNewGame()
        }
    }

    private fun updateWin(delta: Float) {
        stateTimer += delta
        if (stateTimer >= 2f) {
            if (currentLevel < mapFiles.size - 1) {
                currentLevel++
                startNewGame(keepScore = true)
            } else {
                currentLevel = 0
                startNewGame()
            }
        }
    }

    private fun setupExtraWalls() {
        extraWalls.clear()
        val extras = wallRegions.drop(1)
        if (extras.isEmpty()) return

        val wallCells = mutableListOf<GridPoint2>()
        for (row in 0 until maze.height) {
            for (col in 0 until maze.width) {
                if (maze.grid[row][col] == '#') wallCells.add(GridPoint2(col, row))
            }
        }
        if (wallCells.isEmpty()) return

        val count = Random.nextInt(theme.extrasCountMin, theme.extrasCountMax + 1).coerceAtMost(wallCells.size)
        wallCells.shuffle()
        for (i in 0 until count) {
            extraWalls[wallCells[i]] = extras.random()
        }
    }

    private fun drawBackgroundAndWalls() {
        spriteBatch.begin()

        // Фон — тайлы по всей площади лабиринта
        for (row in 0 until maze.height) {
            for (col in 0 until maze.width) {
                val x = Constants.MAZE_OFFSET_X + col * Constants.CELL_SIZE
                val y = Constants.MAZE_OFFSET_Y + (Constants.MAZE_ROWS - 1 - row) * Constants.CELL_SIZE
                spriteBatch.draw(backgroundRegion, x, y, Constants.CELL_SIZE, Constants.CELL_SIZE)
            }
        }

        // Стены: основной спрайт wall_0, в случайных клетках — дополнительный (wall_1..n)
        for (row in 0 until maze.height) {
            for (col in 0 until maze.width) {
                if (maze.grid[row][col] == '#') {
                    val x = Constants.MAZE_OFFSET_X + col * Constants.CELL_SIZE
                    val y = Constants.MAZE_OFFSET_Y + (Constants.MAZE_ROWS - 1 - row) * Constants.CELL_SIZE
                    val region = extraWalls[GridPoint2(col, row)] ?: wallRegions[0]
                    spriteBatch.draw(region, x, y, Constants.CELL_SIZE, Constants.CELL_SIZE)
                }
            }
        }

        spriteBatch.end()
    }

    private fun drawPellets() {
        spriteBatch.begin()
        for (pellet in pellets) {
            pellet.draw(spriteBatch, cheeseRegion)
        }
        spriteBatch.end()
    }

    private fun drawEntities() {
        // Мышь и коты — спрайтами с поворотом по направлению
        spriteBatch.begin()
        mouse.draw(spriteBatch, mouseRegion)
        cats.forEach { cat ->
            cat.draw(spriteBatch, regionFor(cat))
        }
        spriteBatch.end()
    }

    private fun regionFor(cat: Cat): TextureRegion = when (cat.strategy) {
        CatStrategy.Chaser -> catChaserRegion
        CatStrategy.Patrol -> catPatrolRegion
        CatStrategy.Random -> catRandomRegion
        CatStrategy.Ambush -> catAmbushRegion
    }

    private fun drawHud() {
        val eatenCount = pellets.count { it.eaten }

        // Цветные кружки легенды в левом верхнем углу
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        val legendY = listOf(
            Constants.WORLD_HEIGHT - 120f,
            Constants.WORLD_HEIGHT - 145f,
            Constants.WORLD_HEIGHT - 170f,
            Constants.WORLD_HEIGHT - 195f
        )
        cats.forEachIndexed { index, cat ->
            shapeRenderer.color = cat.color
            shapeRenderer.circle(25f, legendY[index], 8f)
        }
        shapeRenderer.end()

        // Текст HUD в левом верхнем углу
        spriteBatch.begin()
        font.color = Color.WHITE
        font.draw(spriteBatch, "Score: $score", 20f, Constants.WORLD_HEIGHT - 30f)
        font.draw(spriteBatch, "Pellets: $eatenCount/${pellets.size}", 20f, Constants.WORLD_HEIGHT - 55f)

        cats.forEachIndexed { index, cat ->
            font.draw(spriteBatch, "${cat.name}: ${catchCount[cat.name] ?: 0}", 45f, legendY[index] + 5f)
        }

        // Номер текущей карты в правом верхнем углу
        font.draw(spriteBatch, "Карта ${currentLevel + 1}/${mapFiles.size}", Constants.WORLD_WIDTH - 160f, Constants.WORLD_HEIGHT - 30f)

        // Версия в левом нижнем углу
        font.draw(spriteBatch, "v${Constants.VERSION}", 20f, 30f)
        spriteBatch.end()
    }

    private fun drawCenteredText(text: String, color: Color) {
        spriteBatch.begin()
        font.color = color
        val layout = GlyphLayout(font, text)
        font.draw(
            spriteBatch,
            text,
            Constants.WORLD_WIDTH / 2f - layout.width / 2f,
            Constants.WORLD_HEIGHT / 2f + layout.height / 2f
        )
        spriteBatch.end()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        shapeRenderer.dispose()
        spriteBatch.dispose()
        backgroundTexture?.dispose()
        wallTextures.forEach { it.dispose() }
        mouseTexture?.dispose()
        cheeseTexture?.dispose()
        catChaserTexture?.dispose()
        catPatrolTexture?.dispose()
        catRandomTexture?.dispose()
        catAmbushTexture?.dispose()
        if (::font.isInitialized) font.dispose()
        Gdx.input.inputProcessor = null
    }

    private enum class GameState {
        PLAYING, GAME_OVER, WIN
    }
}
