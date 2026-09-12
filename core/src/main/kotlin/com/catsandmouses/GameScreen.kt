package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
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
    private lateinit var inputHandler: InputHandler
    private lateinit var maze: MazeLoader
    private lateinit var mouse: Mouse
    private val cats = mutableListOf<Cat>()
    private val pellets = mutableListOf<Pellet>()

    private var score = 0
    private var state = GameState.PLAYING
    private var stateTimer = 0f

    override fun show() {
        camera = OrthographicCamera(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)
        camera.position.set(Constants.WORLD_WIDTH / 2f, Constants.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        viewport = FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, camera)
        shapeRenderer = ShapeRenderer()
        shapeRenderer.projectionMatrix = camera.combined
        spriteBatch = SpriteBatch()
        spriteBatch.projectionMatrix = camera.combined
        font = BitmapFont()
        inputHandler = InputHandler()

        startNewGame()
    }

    private fun startNewGame() {
        maze = MazeLoader("maze.txt")
        mouse = Mouse(maze, 13, 8)
        cats.clear()
        cats.add(Cat(maze, 1, 1, CatStrategy.Chaser, Color(1f, 0.125f, 0.125f, 1f)))
        cats.add(Cat(maze, 26, 1, CatStrategy.Patrol, Color(0.125f, 0.375f, 1f, 1f)))
        cats.add(Cat(maze, 1, 13, CatStrategy.Random, Color(0.625f, 0.125f, 0.94f, 1f)))
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

        drawMaze()
        drawEntities()
        drawHud()

        if (state == GameState.GAME_OVER) {
            drawCenteredText("GAME OVER", Color.RED)
        } else if (state == GameState.WIN) {
            drawCenteredText("YOU WIN", Color.GREEN)
        }
    }

    private fun updatePlaying(delta: Float) {
        mouse.setNextDirection(inputHandler.getDirection())
        mouse.update(delta)

        // Eat pellets
        for (pellet in pellets) {
            if (!pellet.eaten && pellet.cell == mouse.cell) {
                pellet.eat()
                score += 100
            }
        }

        // Update cats
        cats.forEach { it.update(delta, mouse.cell, cats) }

        // Check collision with cats
        for (cat in cats) {
            val dx = mouse.position.x - cat.position.x
            val dy = mouse.position.y - cat.position.y
            val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (dist < Mouse.RADIUS + Cat.RADIUS) {
                state = GameState.GAME_OVER
                stateTimer = 0f
                break
            }
        }

        // Check win
        if (pellets.all { it.eaten }) {
            state = GameState.WIN
            stateTimer = 0f
        }
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

    private fun drawMaze() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        for (row in 0 until maze.height) {
            for (col in 0 until maze.width) {
                val worldPos = maze.cellToWorld(col, row)
                val x = worldPos.x - Constants.CELL_SIZE / 2f
                val y = worldPos.y - Constants.CELL_SIZE / 2f

                when (maze.grid[row][col]) {
                    '#' -> {
                        shapeRenderer.color = Color.BLUE
                        shapeRenderer.rect(x, y, Constants.CELL_SIZE, Constants.CELL_SIZE)
                    }
                }
            }
        }

        // Draw pellets
        for (pellet in pellets) {
            pellet.draw(shapeRenderer)
        }

        shapeRenderer.end()
    }

    private fun drawEntities() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        mouse.draw(shapeRenderer)
        cats.forEach { it.draw(shapeRenderer) }
        shapeRenderer.end()
    }

    private fun drawHud() {
        val eatenCount = pellets.count { it.eaten }

        // Легенда вверху справа: кружки цветов котов
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        val legendX = Constants.WORLD_WIDTH - 130f
        val lineHeight = 25f
        var y = Constants.WORLD_HEIGHT - 30f
        for (cat in cats) {
            shapeRenderer.color = cat.color
            shapeRenderer.circle(legendX, y, 8f)
            y -= lineHeight
        }
        shapeRenderer.end()

        // Текст HUD и подписи легенды
        spriteBatch.begin()
        font.color = Color.WHITE
        font.draw(spriteBatch, "Score: $score", 20f, Constants.WORLD_HEIGHT - 20f)
        font.draw(spriteBatch, "Pellets: $eatenCount/3", 20f, Constants.WORLD_HEIGHT - 45f)

        val labelX = legendX + 18f
        y = Constants.WORLD_HEIGHT - 30f + 5f
        for (cat in cats) {
            val label = when (cat.strategy) {
                is CatStrategy.Chaser -> "Chaser"
                is CatStrategy.Patrol -> "Patrol"
                is CatStrategy.Random -> "Random"
            }
            font.draw(spriteBatch, label, labelX, y)
            y -= lineHeight
        }

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
        font.dispose()
    }

    private enum class GameState {
        PLAYING, GAME_OVER, WIN
    }
}
