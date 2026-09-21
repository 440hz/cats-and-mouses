package com.catsandmouses.lwjgl3

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport
import com.catsandmouses.Constants
import com.catsandmouses.EditorFiles
import com.catsandmouses.GameScreen
import com.catsandmouses.MainGame
import com.catsandmouses.MapEditor
import com.catsandmouses.MazeLoader
import com.catsandmouses.MazeRenderer
import com.catsandmouses.ThemeLoader
import com.catsandmouses.entities.Cat
import com.catsandmouses.entities.Mouse
import com.catsandmouses.entities.Pellet
import com.catsandmouses.graphics.CheeseAnimation
import com.catsandmouses.graphics.HeartAnimation
import com.catsandmouses.graphics.IdleAnimator
import java.io.File
import kotlin.random.Random

/** Символ инструмента «сыр» (значение цифры игре не важно, при сохранении сыр нумеруется `1..N`). */
private const val CHEESE_CHAR = '1'

/** Символ инструмента «сердечко» (латинская H, от HEART). */
private const val HEART_CHAR = 'H'

/**
 * Desktop-редактор карт (запуск `--editor`). WYSIWYG-рендер через [MazeRenderer],
 * правка существующих карт, сохранение в исходные `assets/maps` и playtest.
 *
 * Не входит в APK (код только в модуле lwjgl3).
 */
class EditorScreen(
    private val game: MainGame,
    private val assetsDirArg: String? = null
) : Screen {

    // Геометрия тулбара (верхняя полоса над лабиринтом)
    private val toolbarY = Constants.MAZE_HEIGHT
    private val swatchSize = 44f
    private val swatchGap = 6f
    private val buttonY = 664f
    private val buttonW = 104f
    private val buttonH = 44f

    private val palette = listOf('#', ' ', CHEESE_CHAR, HEART_CHAR, '+', 'A', 'B', 'C', 'D')

    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var batch: SpriteBatch
    private lateinit var shape: ShapeRenderer
    private lateinit var font: BitmapFont
    private lateinit var charFont: BitmapFont
    private lateinit var titleFont: BitmapFont

    private val mazeRenderer = MazeRenderer()
    private val editor = MapEditor()

    private var theme: ThemeLoader? = null

    private var mouseTexture: Texture? = null
    private var cheeseTexture: Texture? = null
    private var heartTexture: Texture? = null
    private val catTextures = mutableListOf<Texture>()
    private var mouseRegion: TextureRegion? = null
    private var cheeseRegion: TextureRegion? = null
    private var heartRegion: TextureRegion? = null
    private val catRegions = mutableMapOf<Char, TextureRegion>()

    private var assetsDir: File? = null
    private var levelIndex = 0
    private var cachedMaze: MazeLoader? = null

    private var selectedChar = '#'
    private var showGrid = false
    /** Максимум сыров: формат карты хранит цифры `1..9`. */
    private val maxCheese = 9
    /** Максимум сердечек на карте (см. `Constants.MAX_HEARTS_PER_MAP`). */
    private val maxHearts = Constants.MAX_HEARTS_PER_MAP

    private var painting = false
    private var paintChar = '#'

    private var hoverCol = -1
    private var hoverRow = -1

    /** Индекс свотча под курсором для тултипа (-1 — нет). */
    private var tooltipSwatch = -1

    private var statusMessage: String? = null
    private var statusUntil = 0f

    /** Аниматоры маркеров по клетке: создаются лениво и живут между кадрами. */
    private val markerAnimators = HashMap<GridPoint2, IdleAnimator>()

    private var dialog: EditorDialog? = null

    private var playtestScreen: GameScreen? = null
    private var initialized = false

    private val swatchRects: List<Rectangle> = palette.mapIndexed { i, _ ->
        Rectangle(16f + i * (swatchSize + swatchGap), buttonY, swatchSize, swatchSize)
    }
    private val playRect = Rectangle(Constants.WORLD_WIDTH - 16f - buttonW, buttonY, buttonW, buttonH)
    private val saveRect = Rectangle(playRect.x - 8f - buttonW, buttonY, buttonW, buttonH)
    private val prevRect = Rectangle(860f, buttonY, buttonH, buttonH)
    private val nextRect = Rectangle(990f, buttonY, buttonH, buttonH)

    private val tmpVec = Vector3()

    // ------------------------------------------------------------------ Screen

    override fun show() {
        if (!initialized) {
            initialized = true
            initRendering()
            initTextures()
            assetsDir = EditorFiles.resolveAssetsDir(
                assetsDirArg,
                System.getProperty("assets.dir"),
                File(System.getProperty("user.dir"))
            )
            loadLevel(game.catalog.indexOfMap("1/1"))
            if (assetsDir == null) {
                status("папка assets не найдена — режим только чтение")
            }
        }
        Gdx.input.inputProcessor = inputAdapter
        // Перехват закрытия окна (крестик) — сначала подтверждение
        EditorWindowClose.handler = { requestClose() }
    }

    private fun initRendering() {
        camera = OrthographicCamera(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)
        camera.position.set(Constants.WORLD_WIDTH / 2f, Constants.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        viewport = FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, camera)
        batch = SpriteBatch().apply { projectionMatrix = camera.combined }
        shape = ShapeRenderer().apply { projectionMatrix = camera.combined }
        font = generateFont(Constants.FONT_SIZE_HUD)
        charFont = generateFont(24)
        titleFont = generateFont(Constants.FONT_SIZE_OVERLAY)
    }

    private fun initTextures() {
        val nearest = Texture.TextureFilter.Nearest
        fun load(path: String): Texture = Texture(Gdx.files.internal(path)).apply { setFilter(nearest, nearest) }

        mouseTexture = load("sprites/mouse.png")
        cheeseTexture = load("sprites/cheese.png")
        heartTexture = load("sprites/heart_full.png")
        mouseRegion = TextureRegion(mouseTexture)
        cheeseRegion = TextureRegion(cheeseTexture)
        heartRegion = TextureRegion(heartTexture)

        val cats = listOf('A' to "sprites/cat_chaser.png", 'B' to "sprites/cat_patrol.png",
            'C' to "sprites/cat_random.png", 'D' to "sprites/cat_ambush.png")
        for ((letter, path) in cats) {
            val texture = load(path)
            catTextures.add(texture)
            catRegions[letter] = TextureRegion(texture)
        }
    }

    private fun generateFont(size: Int): BitmapFont {
        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/boldpixels.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = size
        parameter.borderWidth = size / 16f
        parameter.borderColor = Constants.FONT_COLOR_OUTLINE
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS +
            "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюя" +
            "0123456789:/% ()"
        val generated = generator.generateFont(parameter)
        generator.dispose()
        return generated
    }

    override fun render(delta: Float) {
        updateHover()
        // Анимация маркеров выбранного типа (пауза при открытом диалоге)
        if (dialog == null) {
            markerAnimators.values.forEach { it.update(delta) }
        }
        val pointer = worldCoords(Gdx.input.x, Gdx.input.y)
        tooltipSwatch = if (dialog == null) swatchRects.indexOfFirst { it.contains(pointer.x, pointer.y) } else -1
        val maze = ensureMaze()

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // 1) Фон и стены
        batch.begin()
        mazeRenderer.drawBackground(batch)
        mazeRenderer.drawWalls(batch, maze)
        batch.end()

        // 2) Сетка, курсор, рамка заливки
        shape.begin(ShapeRenderer.ShapeType.Line)
        if (showGrid) drawGrid()
        drawHover()
        shape.end()

        // 3) Фоны тулбара/кнопок/свотчей
        drawToolbarBackgrounds()

        // 4) Маркеры карты и содержимое тулбара
        batch.begin()
        drawMarkers(maze)
        drawToolbarContent()
        drawVersion()
        batch.end()

        // 5) Диалог поверх всего
        drawDialog()
    }

    // ------------------------------------------------------------------ Рендер

    private fun drawGrid() {
        shape.color = Color(1f, 1f, 1f, 0.12f)
        for (col in 0..Constants.MAZE_COLS) {
            val x = Constants.MAZE_OFFSET_X + col * Constants.CELL_SIZE
            shape.line(x, Constants.MAZE_OFFSET_Y, x, Constants.MAZE_OFFSET_Y + Constants.MAZE_HEIGHT)
        }
        for (row in 0..Constants.MAZE_ROWS) {
            val y = Constants.MAZE_OFFSET_Y + row * Constants.CELL_SIZE
            shape.line(Constants.MAZE_OFFSET_X, y, Constants.MAZE_OFFSET_X + Constants.MAZE_WIDTH, y)
        }
    }

    private fun drawHover() {
        if (hoverCol < 0 || hoverRow < 0) return
        val x = Constants.MAZE_OFFSET_X + hoverCol * Constants.CELL_SIZE
        val y = Constants.MAZE_OFFSET_Y + (Constants.MAZE_ROWS - 1 - hoverRow) * Constants.CELL_SIZE
        shape.color = Color(0.78f, 0.54f, 0.32f, 1f)
        shape.rect(x, y, Constants.CELL_SIZE, Constants.CELL_SIZE)
    }

    private fun drawToolbarBackgrounds() {
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0f, 0f, 0f, 0.55f)
        shape.rect(0f, toolbarY, Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT - toolbarY)

        palette.forEachIndexed { i, ch ->
            val r = swatchRects[i]
            shape.color = if (ch == selectedChar) Color(0.78f, 0.54f, 0.32f, 1f) else Color(0.2f, 0.2f, 0.24f, 0.9f)
            shape.rect(r.x, r.y, r.width, r.height)
        }
        drawButtonBg(saveRect)
        drawButtonBg(playRect)
        drawButtonBg(prevRect)
        drawButtonBg(nextRect)

        // Тултип свотча: подложка (текст рисуется в drawToolbarContent)
        tooltipRect()?.let { t ->
            shape.color = Color(0.1f, 0.1f, 0.14f, 0.95f)
            shape.rect(t.x, t.y, t.width, t.height)
            shape.color = Constants.FONT_COLOR_ACCENT
            shape.rect(t.x, t.y, t.width, 2f)
        }
        shape.end()
    }

    /** Текст подсказки для символа палитры. */
    private fun tooltipFor(ch: Char): String = when (ch) {
        '#' -> "Стена"
        ' ' -> "Проход (пусто)"
        CHEESE_CHAR -> "Сыр (до $maxCheese)"
        HEART_CHAR -> "Сердечко (до $maxHearts)"
        '+' -> "Старт мыши"
        in 'A'..'D' -> "Старт кота $ch"
        else -> ch.toString()
    }

    /** Прямоугольник тултипа под наведённым свотчем (null — тултип не нужен). */
    private fun tooltipRect(): Rectangle? {
        val i = tooltipSwatch
        if (i !in palette.indices) return null
        val r = swatchRects[i]
        val w = GlyphLayout(font, tooltipFor(palette[i])).width + 16f
        val h = 26f
        val x = (r.x + r.width / 2f - w / 2f).coerceIn(4f, Constants.WORLD_WIDTH - w - 4f)
        // Под полосой тулбара, поверх лабиринта (над свотчами места нет — впритык к верху экрана)
        val y = toolbarY - h - 6f
        return Rectangle(x, y, w, h)
    }

    private fun drawButtonBg(rect: Rectangle) {
        shape.color = Color(0.2f, 0.2f, 0.24f, 0.95f)
        shape.rect(rect.x, rect.y, rect.width, rect.height)
    }

    /**
     * Анимация маркера для символа: сыр — редкие акценты, сердечко — постоянный пульс.
     * Для мыши и котов — заглушка (null) до отдельных этапов 17.2+.
     */
    private fun animationFactoryFor(ch: Char): ((Long) -> IdleAnimator)? = when (ch) {
        CHEESE_CHAR -> CheeseAnimation::create
        HEART_CHAR -> HeartAnimation::create
        else -> null
    }

    /** Символы одного типа: все цифры `1..9` — сыр (нумерация меняется), остальные — точное совпадение. */
    private fun sameMarkerType(a: Char, b: Char): Boolean =
        (a in '1'..'9' && b in '1'..'9') || a == b

    /** Синхронизирует набор живых аниматоров с картой (лишние/чужие клетки убираются). */
    private fun syncAnimators(maze: MazeLoader) {
        markerAnimators.keys.removeAll { cell ->
            cell.y !in 0 until maze.height || cell.x !in 0 until maze.width ||
                !sameMarkerType(maze.grid[cell.y][cell.x], selectedChar)
        }
    }

    /** Ленивый аниматор клетки (сохраняется между кадрами). */
    private fun animatorAt(col: Int, row: Int, factory: (Long) -> IdleAnimator): IdleAnimator {
        val cell = GridPoint2(col, row)
        return markerAnimators.getOrPut(cell) {
            factory(CheeseAnimation.cellSeed(col, row) xor Random.nextLong())
        }
    }

    private fun markerSize(ch: Char): Float = when {
        ch == '+' -> Mouse.SPRITE_SIZE
        ch in '1'..'9' -> Pellet.SPRITE_SIZE
        ch == HEART_CHAR -> Constants.HEART_SIZE
        else -> Cat.SPRITE_SIZE
    }

    private fun drawMarkers(maze: MazeLoader) {
        // Анимируется только тот тип, чей инструмент выбран (и только если у него есть анимация)
        val animated = animationFactoryFor(selectedChar)
        if (animated == null) {
            markerAnimators.clear()
        } else {
            syncAnimators(maze)
        }
        for (row in 0 until maze.height) {
            for (col in 0 until maze.width) {
                val ch = maze.grid[row][col]
                val region = spriteFor(ch) ?: continue
                // Тот же размер, что и в игре (мышь/коты/сыр — 48), чтобы редактор был WYSIWYG
                val size = markerSize(ch)
                val x = Constants.MAZE_OFFSET_X + col * Constants.CELL_SIZE + (Constants.CELL_SIZE - size) / 2f
                val y = Constants.MAZE_OFFSET_Y + (Constants.MAZE_ROWS - 1 - row) * Constants.CELL_SIZE +
                    (Constants.CELL_SIZE - size) / 2f
                if (animated != null && sameMarkerType(ch, selectedChar)) {
                    val transform = animatorAt(col, row, animated).transform()
                    val half = size / 2f
                    batch.draw(
                        region, x, y, half, half, size, size,
                        transform.scale, transform.scale, transform.rotation
                    )
                } else {
                    batch.draw(region, x, y, size, size)
                }
            }
        }
    }

    private fun spriteFor(ch: Char): TextureRegion? = when {
        ch == '+' -> mouseRegion
        ch in '1'..'9' -> cheeseRegion
        ch == HEART_CHAR -> heartRegion
        ch in 'A'..'D' -> catRegions[ch]
        else -> null
    }

    private fun drawToolbarContent() {
        font.color.set(Constants.FONT_COLOR_TEXT)

        // Свотчи: превью + символ
        palette.forEachIndexed { i, ch ->
            val r = swatchRects[i]
            val region = if (ch == '#') mazeRenderer.wallPreviewRegion() else spriteFor(ch)
            if (region != null) {
                batch.draw(region, r.x + 4f, r.y + 4f, r.width - 8f, r.height - 8f)
            }
            charFont.color.set(Color.WHITE)
            // У свотчей сыра и сердечка вместо символа — счётчик на карте
            val label = when (ch) {
                CHEESE_CHAR -> editor.cheeseCount().toString()
                HEART_CHAR -> editor.heartCount().toString()
                else -> ch.toString()
            }
            drawCentered(charFont, label, r.x + r.width / 2f, r.y + 18f)
        }

        drawCentered(font, "Сохранить", saveRect.x + saveRect.width / 2f, saveRect.y + 28f)
        drawCentered(font, "Играть", playRect.x + playRect.width / 2f, playRect.y + 28f)
        drawCentered(charFont, "<", prevRect.x + prevRect.width / 2f, prevRect.y + 18f)
        drawCentered(charFont, ">", nextRect.x + nextRect.width / 2f, nextRect.y + 18f)

        val mapName = game.catalog.mapName(levelIndex)
        val themeName = theme?.activeTheme ?: "-"
        val nameCenter = (prevRect.x + prevRect.width + nextRect.x) / 2f
        drawCentered(font, "$mapName  $themeName", nameCenter, buttonY + 28f)

        // Тултип наведённого свотча
        tooltipRect()?.let { t ->
            font.color.set(Constants.FONT_COLOR_TEXT)
            font.draw(batch, tooltipFor(palette[tooltipSwatch]), t.x + 8f, t.y + t.height - 7f)
        }

        // Статус: сообщение о сохранении, иначе предупреждения валидации
        val validation = editor.validate()
        val warnings = validation.warnings().joinToString("; ")
        val info = statusMessage ?: warnings.ifEmpty { defaultHint() }
        val isError = statusMessage == null && !validation.isOk
        font.color.set(if (isError) Color(1f, 0.45f, 0.4f, 1f) else Constants.FONT_COLOR_TEXT)
        font.draw(batch, info, 16f, toolbarY + 14f)
    }

    private fun defaultHint(): String =
        "Сыр: ${editor.cheeseCount()}/$maxCheese · Сердца: ${editor.heartCount()}/$maxHearts · " +
            "ЛКМ рисовать, ПКМ стереть, G сетка, Ctrl+S сохранить (+PNG), F5 играть"

    private fun drawVersion() {
        font.color.set(Constants.FONT_COLOR_TEXT)
        val text = "v${Constants.VERSION} редактор"
        val layout = GlyphLayout(font, text)
        font.draw(batch, text, Constants.WORLD_WIDTH - layout.width - 8f, 20f)
    }

    private fun drawDialog() {
        val d = dialog ?: return
        val panel = dialogPanelRect(d)
        val rects = dialogButtonRects(d)

        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0f, 0f, 0f, 0.6f)
        shape.rect(0f, 0f, Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)
        shape.color = Color(0.16f, 0.16f, 0.2f, 1f)
        shape.rect(panel.x, panel.y, panel.width, panel.height)
        rects.forEach { r ->
            shape.color = Color(0.24f, 0.24f, 0.28f, 1f)
            shape.rect(r.x, r.y, r.width, r.height)
        }
        shape.end()

        batch.begin()
        titleFont.color.set(Constants.FONT_COLOR_TEXT)
        drawCentered(titleFont, d.title, panel.x + panel.width / 2f, panel.y + panel.height - 24f)
        font.color.set(Constants.FONT_COLOR_TEXT)
        d.buttons.forEachIndexed { i, (label, _) ->
            val r = rects[i]
            val color = if (i == d.focused) Constants.FONT_COLOR_ACCENT else Constants.FONT_COLOR_TEXT
            font.color.set(color)
            drawCentered(font, label, r.x + r.width / 2f, r.y + 28f)
        }
        batch.end()
    }

    private fun drawCentered(f: BitmapFont, text: String, cx: Float, baselineY: Float) {
        val layout = GlyphLayout(f, text)
        f.draw(batch, text, cx - layout.width / 2f, baselineY)
    }

    // ------------------------------------------------------------------ Логика

    private fun ensureMaze(): MazeLoader {
        cachedMaze?.let { return it }
        return MazeLoader.fromText(editor.toText()).also { cachedMaze = it }
    }

    private fun invalidateMaze() {
        cachedMaze = null
    }

    private fun loadLevel(index: Int) {
        val count = game.catalog.count
        levelIndex = ((index % count) + count) % count
        val mapName = game.catalog.mapName(levelIndex)
        val dir = assetsDir
        val text = when {
            dir != null -> File(dir, "maps/$mapName.txt").let { if (it.exists()) it.readText() else "" }
            else -> try {
                Gdx.files.internal("maps/$mapName.txt").readString()
            } catch (_: Exception) {
                ""
            }
        }
        editor.load(text)
        invalidateMaze()

        theme = ThemeLoader.forId(game.catalog.themeFor(levelIndex))
        theme?.let { mazeRenderer.loadTheme(it) }
        statusMessage = null
    }

    private fun save(showDialog: Boolean = true) {
        val dir = assetsDir
        if (dir == null) {
            status("папка assets не найдена — сохранение недоступно")
            return
        }
        // Канонический вид: сыр нумеруется 1..N построчно
        editor.normalizeCheese()
        invalidateMaze()

        val mapName = game.catalog.mapName(levelIndex)
        val rel = "maps/$mapName.txt"
        EditorFiles.saveMap(dir, rel, editor.toText())
        editor.markSaved()

        // Рядом с картой сохраняем превью PNG (тот же рендер, что в игре)
        val previewError = runCatching { exportPreview(dir, mapName) }.exceptionOrNull()

        if (showDialog) {
            // Явное подтверждение сохранения (MENU.md не затрагивается — это dev-редактор)
            dialog = EditorDialog(
                title = if (previewError == null) "Карта сохранена" else "Сохранено (нет превью)",
                buttons = listOf("ОК" to { dialog = null })
            )
        } else {
            status(
                if (previewError == null) "Сохранено: $rel (+ $mapName.png)"
                else "Сохранено: $rel (превью не создалось: ${previewError.message})"
            )
        }
    }

    /**
     * Отрендерить карту 1:1 (фон, стены, маркеры) в offscreen-буфер и сохранить
     * PNG рядом с `.txt` — то же превью, что делает `tools/map_preview.py`.
     */
    private fun exportPreview(dir: File, mapName: String) {
        val width = Constants.MAZE_WIDTH.toInt()
        val height = Constants.MAZE_HEIGHT.toInt()
        val fbo = FrameBuffer(Pixmap.Format.RGBA8888, width, height, false)
        try {
            fbo.begin()
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

            batch.projectionMatrix = camera.combined
            batch.begin()
            mazeRenderer.drawBackground(batch)
            mazeRenderer.drawWalls(batch, ensureMaze())
            drawMarkers(ensureMaze())
            batch.end()

            // Пиксели GL читаются снизу вверх — разворачиваем в изображение
            val pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height)
            flipVertically(pixmap)
            val out = File(dir, "maps/$mapName.png")
            PixmapIO.writePNG(Gdx.files.absolute(out.absolutePath), pixmap)
            pixmap.dispose()
        } finally {
            fbo.end()
            fbo.dispose()
        }
    }

    /** Разворот Pixmap по вертикали (GL-буфер снизу вверх → изображение сверху вниз). */
    private fun flipVertically(pixmap: Pixmap) {
        val rowBytes = pixmap.width * 4
        val buffer = pixmap.pixels
        val row = ByteArray(rowBytes)
        val other = ByteArray(rowBytes)
        for (y in 0 until pixmap.height / 2) {
            val top = y * rowBytes
            val bottom = (pixmap.height - 1 - y) * rowBytes
            buffer.position(top); buffer.get(row)
            buffer.position(bottom); buffer.get(other)
            buffer.position(top); buffer.put(other)
            buffer.position(bottom); buffer.put(row)
        }
    }

    private fun play() {
        val screen = GameScreen(
            systemTheme = game.systemTheme,
            catalog = game.catalog,
            debugStartMap = game.catalog.mapName(levelIndex),
            overrideMazeText = editor.toText(),
            saveProgress = false,
            onExitOverride = { backToEditor() },
            countdownEnabled = false,
            mainGame = game
        )
        playtestScreen = screen
        game.setScreen(screen)
    }

    private fun backToEditor() {
        val playtest = playtestScreen ?: return
        playtestScreen = null
        // onExitOverride вызывается из GameScreen.render(); освобождать экран и
        // переключаться нужно после текущего кадра, иначе рендер продолжит работу
        // с уже освобождёнными ресурсами и приложение упадёт.
        Gdx.app.postRunnable {
            playtest.dispose()
            game.setScreen(this)
        }
    }

    private fun requestSwitch(target: Int) {
        if (!editor.dirty) {
            loadLevel(target)
            return
        }
        dialog = EditorDialog(
            title = "Сохранить изменения?",
            buttons = listOf(
                "Сохранить" to { dialog = null; save(showDialog = false); loadLevel(target) },
                "Не сохранять" to { dialog = null; loadLevel(target) },
                "Отмена" to { dialog = null }
            )
        )
    }

    private fun confirmExit() {
        val suffix = if (editor.dirty) " (изменения не сохранены)" else ""
        dialog = EditorDialog(
            title = "Выходите!$suffix",
            buttons = listOf(
                "Да" to { dialog = null; Gdx.app.exit() },
                "Нет" to { dialog = null }
            )
        )
    }

    /**
     * Запрос на закрытие окна (крестик). true — закрытие отменяется,
     * показываем подтверждение «Выходите!».
     */
    private fun requestClose(): Boolean {
        if (dialog == null) confirmExit()
        return true
    }

    private fun status(message: String) {
        statusMessage = message
        statusUntil = runTime() + 2.5f
    }

    private fun runTime(): Float = System.nanoTime() / 1_000_000_000f

    private fun updateHover() {
        val w = worldCoords(Gdx.input.x, Gdx.input.y)
        val cell = cellAt(w.x, w.y)
        if (cell != null) {
            hoverCol = cell.first
            hoverRow = cell.second
        } else {
            hoverCol = -1
            hoverRow = -1
        }
        if (statusMessage != null && runTime() > statusUntil) statusMessage = null
    }

    // ------------------------------------------------------------------ Ввод

    private val inputAdapter = object : InputAdapter() {

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val w = worldCoords(screenX, screenY)
            dialog?.let { d ->
                handleDialogClick(d, w.x, w.y)
                return true
            }
            if (w.y >= toolbarY) {
                handleToolbarClick(w.x, w.y)
                return true
            }
            val cell = cellAt(w.x, w.y) ?: return false
            paintChar = if (button == Input.Buttons.RIGHT) ' ' else selectedChar
            painting = true
            paintCell(cell.first, cell.second)
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            val w = worldCoords(screenX, screenY)
            val cell = cellAt(w.x, w.y) ?: return false
            if (painting) {
                paintCell(cell.first, cell.second)
            }
            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            painting = false
            return false
        }

        override fun keyDown(keycode: Int): Boolean {
            dialog?.let { d ->
                when (keycode) {
                    Input.Keys.UP -> d.moveFocus(-1)
                    Input.Keys.DOWN -> d.moveFocus(1)
                    Input.Keys.ENTER, Input.Keys.SPACE -> d.activate()
                    Input.Keys.ESCAPE, Input.Keys.BACK -> dialog = null
                    else -> return true
                }
                return true
            }

            val ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)
            return when {
                keycode == Input.Keys.ESCAPE -> { confirmExit(); true }
                ctrl && keycode == Input.Keys.S -> { save(); true }
                ctrl && keycode == Input.Keys.Z -> { editor.undo(); invalidateMaze(); true }
                ctrl && keycode == Input.Keys.Y -> { editor.redo(); invalidateMaze(); true }
                keycode == Input.Keys.F5 -> { play(); true }
                keycode == Input.Keys.G -> {
                    showGrid = !showGrid
                    status(if (showGrid) "Сетка: вкл" else "Сетка: выкл")
                    true
                }
                keycode == Input.Keys.COMMA || keycode == Input.Keys.PAGE_UP -> { requestSwitch(levelIndex - 1); true }
                keycode == Input.Keys.PERIOD || keycode == Input.Keys.PAGE_DOWN -> { requestSwitch(levelIndex + 1); true }
                keycode == Input.Keys.SPACE -> { selectedChar = ' '; true }
                keycode == Input.Keys.POUND -> { selectedChar = '#'; true }
                keycode == Input.Keys.PLUS || keycode == Input.Keys.EQUALS -> { selectedChar = '+'; true }
                keycode == Input.Keys.NUM_1 -> { selectedChar = CHEESE_CHAR; true }
                keycode == Input.Keys.H -> { selectedChar = HEART_CHAR; true }
                keycode in Input.Keys.A..Input.Keys.D -> {
                    selectedChar = ('A' + (keycode - Input.Keys.A))
                    true
                }
                else -> false
            }
        }
    }

    /** Нанести текущий символ кисти в клетку. ПКМ (символ `' '`) стирает. */
    private fun paintCell(col: Int, row: Int) {
        when {
            paintChar == CHEESE_CHAR -> placeLimited(col, row, CHEESE_CHAR, editor.cheeseCount(), maxCheese, "сыров")
            paintChar == HEART_CHAR -> placeLimited(col, row, HEART_CHAR, editor.heartCount(), maxHearts, "сердец")
            paintChar in 'A'..'D' -> {
                // Кот: ровно один старт на букву — при переносе старый стирается
                editor.setCatStart(col, row, paintChar)
                invalidateMaze()
            }
            else -> {
                editor.setCell(col, row, paintChar)
                invalidateMaze()
            }
        }
    }

    /** Кисть с лимитом (сыр/сердечко): только на пустую `' '`, сверх лимита — статус и игнор. */
    private fun placeLimited(col: Int, row: Int, ch: Char, current: Int, limit: Int, label: String) {
        val cell = editor.grid.getOrNull(row)?.getOrNull(col) ?: return
        if (cell != ' ') return
        if (current >= limit) {
            status("Максимум $limit $label")
            return
        }
        editor.setCell(col, row, ch)
        invalidateMaze()
    }

    private fun handleToolbarClick(x: Float, y: Float): Boolean {
        when {
            saveRect.contains(x, y) -> save()
            playRect.contains(x, y) -> play()
            prevRect.contains(x, y) -> requestSwitch(levelIndex - 1)
            nextRect.contains(x, y) -> requestSwitch(levelIndex + 1)
            else -> {
                swatchRects.forEachIndexed { i, r -> if (r.contains(x, y)) selectedChar = palette[i] }
            }
        }
        return true
    }

    private fun handleDialogClick(d: EditorDialog, x: Float, y: Float) {
        val rects = dialogButtonRects(d)
        rects.forEachIndexed { i, r ->
            if (r.contains(x, y)) {
                d.focused = i
                d.activate()
            }
        }
    }

    private fun dialogPanelRect(d: EditorDialog): Rectangle {
        val width = 520f
        val height = 90f + d.buttons.size * 64f
        return Rectangle(
            Constants.WORLD_WIDTH / 2f - width / 2f,
            Constants.WORLD_HEIGHT / 2f - height / 2f,
            width,
            height
        )
    }

    private fun dialogButtonRects(d: EditorDialog): List<Rectangle> {
        val panel = dialogPanelRect(d)
        val w = 360f
        val h = 48f
        val gap = 12f
        var y = panel.y + panel.height - 70f - h
        return d.buttons.map { _ ->
            val rect = Rectangle(panel.x + panel.width / 2f - w / 2f, y, w, h)
            y -= h + gap
            rect
        }
    }

    // ------------------------------------------------------------------ Утилиты

    private fun worldCoords(screenX: Int, screenY: Int): Vector2 {
        tmpVec.set(screenX.toFloat(), screenY.toFloat(), 0f)
        viewport.unproject(tmpVec)
        return Vector2(tmpVec.x, tmpVec.y)
    }

    private fun cellAt(x: Float, y: Float): Pair<Int, Int>? {
        val maxX = Constants.MAZE_OFFSET_X + Constants.MAZE_WIDTH
        val maxY = Constants.MAZE_OFFSET_Y + Constants.MAZE_HEIGHT
        if (x < Constants.MAZE_OFFSET_X || x >= maxX || y < Constants.MAZE_OFFSET_Y || y >= maxY) return null
        val col = ((x - Constants.MAZE_OFFSET_X) / Constants.CELL_SIZE).toInt()
        val row = (Constants.MAZE_ROWS - 1) - ((y - Constants.MAZE_OFFSET_Y) / Constants.CELL_SIZE).toInt()
        return col to row
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {
        // Во время playtest редактор скрыт — перехват закрытия окна не нужен
        EditorWindowClose.handler = null
    }

    override fun dispose() {
        EditorWindowClose.handler = null
        playtestScreen?.dispose()
        playtestScreen = null
        batch.dispose()
        shape.dispose()
        font.dispose()
        charFont.dispose()
        titleFont.dispose()
        mouseTexture?.dispose()
        cheeseTexture?.dispose()
        heartTexture?.dispose()
        catTextures.forEach { it.dispose() }
        mazeRenderer.dispose()
        if (Gdx.input.inputProcessor === inputAdapter) {
            Gdx.input.inputProcessor = null
        }
    }

    /** Простое модальное окно редактора: заголовок и вертикальный список кнопок. */
    private class EditorDialog(
        val title: String,
        val buttons: List<Pair<String, () -> Unit>>
    ) {
        var focused = 0

        fun moveFocus(delta: Int) {
            if (buttons.isEmpty()) return
            focused = ((focused + delta) % buttons.size + buttons.size) % buttons.size
        }

        fun activate() {
            buttons.getOrNull(focused)?.second?.invoke()
        }
    }
}
