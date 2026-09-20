package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.GridPoint2
import kotlin.random.Random

/**
 * Отрисовка лабиринта: фон темы, стены (классические или dual-grid autotile)
 * и случайные доп-стены. Общий рендер для [GameScreen] и редактора карт.
 *
 * Владеет текстурами темы уровня и освобождает их в [dispose].
 */
class MazeRenderer {

    private var backgroundTexture: Texture? = null
    private var wallAutotileTexture: Texture? = null
    private val wallTextures = mutableListOf<Texture>()
    private val wallRegions = mutableListOf<TextureRegion>()
    private val wallAutotileRegions = mutableListOf<TextureRegion>()
    private val extraWalls = mutableMapOf<GridPoint2, TextureRegion>()

    private lateinit var backgroundRegion: TextureRegion

    /** true, если активная тема использует dual-grid autotile. */
    var hasAutotile = false
        private set

    /** id активной темы (для логов). */
    var themeId: String? = null
        private set

    /** Загрузить (или перезагрузить) текстуры темы уровня; старые освобождаются. */
    fun loadTheme(theme: ThemeLoader) {
        disposeTheme()

        themeId = theme.activeTheme
        hasAutotile = theme.hasAutotile
        val nearest = Texture.TextureFilter.Nearest

        backgroundTexture = Texture(Gdx.files.internal(theme.backgroundPath)).apply {
            setFilter(nearest, nearest)
        }
        backgroundRegion = TextureRegion(backgroundTexture)

        if (theme.hasAutotile) {
            val texture = Texture(Gdx.files.internal(theme.wallAutotilePath))
            texture.setFilter(nearest, nearest)
            wallAutotileTexture = texture
            val tileW = texture.width / theme.atlasColumns
            val tileH = texture.height / theme.atlasRows
            for (row in 0 until theme.atlasRows) {
                for (col in 0 until theme.atlasColumns) {
                    wallAutotileRegions.add(TextureRegion(texture, col * tileW, row * tileH, tileW, tileH))
                }
            }
        } else {
            for (path in theme.wallPaths) {
                val texture = Texture(Gdx.files.internal(path))
                texture.setFilter(nearest, nearest)
                wallTextures.add(texture)
            }
            for (texture in wallTextures) {
                wallRegions.add(TextureRegion(texture))
            }
        }
    }

    /** Раз за карту выбрать случайные стенные клетки под доп-спрайты темы. */
    fun setupExtraWalls(theme: ThemeLoader, maze: MazeLoader) {
        extraWalls.clear()
        if (hasAutotile) return
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

    /** Регион для мини-превью стены (палитра редактора). */
    fun wallPreviewRegion(): TextureRegion? =
        wallAutotileRegions.firstOrNull() ?: wallRegions.firstOrNull()

    /**
     * Фон — тайлы по всему экрану (за лабиринтом остаётся фон, а не чёрная заливка).
     * Тайл рисуется 1:1 в натуральном размере текстуры (например, 200×200).
     */
    fun drawBackground(batch: SpriteBatch) {
        val tileW = backgroundRegion.regionWidth.toFloat()
        val tileH = backgroundRegion.regionHeight.toFloat()
        val stepW = tileW.toInt()
        val stepH = tileH.toInt()
        for (x in 0 until Constants.WORLD_WIDTH.toInt() + stepW step stepW) {
            for (y in 0 until Constants.WORLD_HEIGHT.toInt() + stepH step stepH) {
                batch.draw(backgroundRegion, x.toFloat(), y.toFloat(), tileW, tileH)
            }
        }
    }

    /** Стены: dual-grid autotile либо классический спрайт на каждую клетку. */
    fun drawWalls(batch: SpriteBatch, maze: MazeLoader) {
        if (hasAutotile) {
            drawAutotileWalls(batch, maze)
        } else {
            drawClassicWalls(batch, maze)
        }
    }

    /** Классический рендер: один спрайт на каждую клетку стены + случайные допы. */
    private fun drawClassicWalls(batch: SpriteBatch, maze: MazeLoader) {
        for (row in 0 until maze.height) {
            for (col in 0 until maze.width) {
                if (maze.grid[row][col] == '#') {
                    val x = Constants.MAZE_OFFSET_X + col * Constants.CELL_SIZE
                    val y = Constants.MAZE_OFFSET_Y + (Constants.MAZE_ROWS - 1 - row) * Constants.CELL_SIZE
                    val region = extraWalls[GridPoint2(col, row)] ?: wallRegions[0]
                    batch.draw(region, x, y, Constants.CELL_SIZE, Constants.CELL_SIZE)
                }
            }
        }
    }

    /** Dual-grid 15-piece рендер: тайл рисуется в каждом углу между логическими клетками. */
    private fun drawAutotileWalls(batch: SpriteBatch, maze: MazeLoader) {
        // Маска собирается из 4 логических клеток вокруг угла:
        // 1 — top-left, 2 — top-right, 4 — bottom-left, 8 — bottom-right.
        val frameByMask = intArrayOf(-1, 15, 8, 9, 0, 11, 14, 7, 13, 4, 1, 10, 3, 2, 5, 6)
        for (row in 0..maze.height) {
            for (col in 0..maze.width) {
                val mask =
                    (if (maze.isWall(col - 1, row - 1)) 1 else 0) or
                    (if (maze.isWall(col, row - 1)) 2 else 0) or
                    (if (maze.isWall(col - 1, row)) 4 else 0) or
                    (if (maze.isWall(col, row)) 8 else 0)
                val frame = frameByMask[mask]
                if (frame >= 0) {
                    val region = wallAutotileRegions[frame]
                    val x = Constants.MAZE_OFFSET_X + col * Constants.CELL_SIZE - Constants.CELL_SIZE / 2f
                    val y = Constants.MAZE_OFFSET_Y + (Constants.MAZE_ROWS - row) * Constants.CELL_SIZE - Constants.CELL_SIZE / 2f
                    batch.draw(region, x, y, Constants.CELL_SIZE, Constants.CELL_SIZE)
                }
            }
        }
    }

    private fun disposeTheme() {
        backgroundTexture?.dispose()
        backgroundTexture = null
        wallTextures.forEach { it.dispose() }
        wallTextures.clear()
        wallRegions.clear()
        wallAutotileTexture?.dispose()
        wallAutotileTexture = null
        wallAutotileRegions.clear()
        extraWalls.clear()
    }

    fun dispose() {
        disposeTheme()
    }
}
