package com.catsandmouses

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
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.catsandmouses.entities.Cat
import com.catsandmouses.entities.CatStrategy
import com.catsandmouses.entities.Mouse
import com.catsandmouses.entities.Pellet

class GameScreen : Screen {

    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var font: BitmapFont
    private var grassTexture: Texture? = null
    private var bushTexture: Texture? = null
    private var mouseTexture: Texture? = null
    private lateinit var grassRegion: TextureRegion
    private lateinit var bushRegion: TextureRegion
    private lateinit var mouseRegion: TextureRegion
    private lateinit var inputHandler: InputHandler
    private lateinit var maze: MazeLoader
    private lateinit var mouse: Mouse
    private val cats = mutableListOf<Cat>()
    private val pellets = mutableListOf<Pellet>()

    private var score = 0
    private var state = GameState.PLAYING
    private var stateTimer = 0f
    private var lastCatcher: Cat? = null
    private val catchCount = mutableMapOf<String, Int>()

    override fun show() {
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

        // Текстуры фона, стен и персонажей (пиксель-арт, без сглаживания)
        grassTexture = Texture(Gdx.files.internal("sprites/grass.png"))
        bushTexture = Texture(Gdx.files.internal("sprites/bush.png"))
        mouseTexture = Texture(Gdx.files.internal("sprites/mouse.png"))
        grassTexture!!.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        bushTexture!!.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        mouseTexture!!.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        grassRegion = TextureRegion(grassTexture)
        bushRegion = TextureRegion(bushTexture)
        mouseRegion = TextureRegion(mouseTexture)

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

    private fun startNewGame() {
        maze = MazeLoader("maze.txt")
        mouse = Mouse(maze, 13, 8)
        cats.clear()
        cats.add(Cat(maze, 1, 1, CatStrategy.Chaser, Color(1f, 0.125f, 0.125f, 1f), "Барсик"))
        cats.add(Cat(maze, 26, 1, CatStrategy.Patrol, Color(0.125f, 0.375f, 1f, 1f), "Матроскин"))
        cats.add(Cat(maze, 1, 13, CatStrategy.Random, Color(0.625f, 0.125f, 0.94f, 1f), "Леопольд"))
        pellets.clear()
        spawnPellets()
        score = 0
        state = GameState.PLAYING
        stateTimer = 0f
    }

    private fun spawnPellets() {
        val reachableCells = maze.findReachableCells(13, 8).toMutableList()
        Gdx.app.log("Maze", "Reachable cells: ${reachableCells.size}")

        val occupiedCells = mutableSetOf<GridPoint2>()
        occupiedCells.add(GridPoint2(13, 8)) // mouse start
        cats.forEach { occupiedCells.add(GridPoint2(it.cell.x, it.cell.y)) }

        val availableCells = reachableCells.filter { !occupiedCells.contains(it) }.shuffled()

        val count = minOf(3, availableCells.size)
        if (count < 3) {
            Gdx.app.log("Maze", "Warning: only $count reachable cells available for pellets")
        }

        pellets.clear()
        for (i in 0 until count) {
            pellets.add(Pellet(availableCells[i], maze))
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        when (state) {
            GameState.PLAYING -> updatePlaying(delta)
            GameState.GAME_OVER -> updateGameOver(delta)
            GameState.WIN -> updateWin(delta)
        }

        // 1) Трава и кусты через SpriteBatch
        spriteBatch.projectionMatrix = camera.combined
        drawBackgroundAndWalls()

        // 2) Точки, мышь и коты через ShapeRenderer
        shapeRenderer.projectionMatrix = camera.combined
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
            cat.update(delta, mouse.cell, reservedCells)
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
            startNewGame()
        }
    }

    private fun updateWin(delta: Float) {
        stateTimer += delta
        if (stateTimer >= 2f) {
            startNewGame()
        }
    }

    private fun drawBackgroundAndWalls() {
        spriteBatch.begin()

        // Трава — тайлы по всей площади лабиринта
        for (row in 0 until maze.height) {
            for (col in 0 until maze.width) {
                val x = Constants.MAZE_OFFSET_X + col * Constants.CELL_SIZE
                val y = Constants.MAZE_OFFSET_Y + (Constants.MAZE_ROWS - 1 - row) * Constants.CELL_SIZE
                spriteBatch.draw(grassRegion, x, y, Constants.CELL_SIZE, Constants.CELL_SIZE)
            }
        }

        // Кусты — стены
        for (row in 0 until maze.height) {
            for (col in 0 until maze.width) {
                if (maze.grid[row][col] == '#') {
                    val x = Constants.MAZE_OFFSET_X + col * Constants.CELL_SIZE
                    val y = Constants.MAZE_OFFSET_Y + (Constants.MAZE_ROWS - 1 - row) * Constants.CELL_SIZE
                    spriteBatch.draw(bushRegion, x, y, Constants.CELL_SIZE, Constants.CELL_SIZE)
                }
            }
        }

        spriteBatch.end()
    }

    private fun drawPellets() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Точки
        for (pellet in pellets) {
            pellet.draw(shapeRenderer)
        }

        shapeRenderer.end()
    }

    private fun drawEntities() {
        // Мышь рисуется спрайтом, а коты пока фигурами
        spriteBatch.begin()
        mouse.draw(spriteBatch, mouseRegion)
        spriteBatch.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        cats.forEach { it.draw(shapeRenderer) }
        shapeRenderer.end()
    }

    private fun drawHud() {
        val eatenCount = pellets.count { it.eaten }

        // Цветные кружки легенды в левом верхнем углу
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = cats[0].color
        shapeRenderer.circle(25f, Constants.WORLD_HEIGHT - 120f, 8f)
        shapeRenderer.color = cats[1].color
        shapeRenderer.circle(25f, Constants.WORLD_HEIGHT - 145f, 8f)
        shapeRenderer.color = cats[2].color
        shapeRenderer.circle(25f, Constants.WORLD_HEIGHT - 170f, 8f)
        shapeRenderer.end()

        // Текст HUD в левом верхнем углу
        spriteBatch.begin()
        font.color = Color.WHITE
        font.draw(spriteBatch, "Score: $score", 20f, Constants.WORLD_HEIGHT - 30f)
        font.draw(spriteBatch, "Pellets: $eatenCount/3", 20f, Constants.WORLD_HEIGHT - 55f)

        font.draw(spriteBatch, "${cats[0].name}: ${catchCount[cats[0].name] ?: 0}", 45f, Constants.WORLD_HEIGHT - 115f)
        font.draw(spriteBatch, "${cats[1].name}: ${catchCount[cats[1].name] ?: 0}", 45f, Constants.WORLD_HEIGHT - 140f)
        font.draw(spriteBatch, "${cats[2].name}: ${catchCount[cats[2].name] ?: 0}", 45f, Constants.WORLD_HEIGHT - 165f)

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
        grassTexture?.dispose()
        bushTexture?.dispose()
        mouseTexture?.dispose()
        if (::font.isInitialized) font.dispose()
    }

    private enum class GameState {
        PLAYING, GAME_OVER, WIN
    }
}
