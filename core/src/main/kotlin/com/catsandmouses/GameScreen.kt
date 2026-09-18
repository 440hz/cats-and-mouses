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
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport
import kotlin.random.Random
import com.catsandmouses.entities.Cat
import com.catsandmouses.entities.CatStrategy
import com.catsandmouses.entities.Mouse
import com.catsandmouses.entities.Pellet

class GameScreen(
    private val systemTheme: ThemeLoader,
    private val catalog: LevelCatalog,
    private val themeOverride: String? = null,
    private val debugStartMap: String = "1/1",
    private val debugLogs: Boolean = false,
    private val initialScore: Int = 0,
    private val mainGame: MainGame
) : Screen {

    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var hudFont: BitmapFont
    private lateinit var menuTitleFont: BitmapFont
    private lateinit var buttonFont: BitmapFont
    private var backgroundTexture: Texture? = null
    private val wallTextures = mutableListOf<Texture>()
    private var mouseTexture: Texture? = null
    private var cheeseTexture: Texture? = null
    private var catChaserTexture: Texture? = null
    private var catPatrolTexture: Texture? = null
    private var catRandomTexture: Texture? = null
    private var catAmbushTexture: Texture? = null
    private var heartFullTexture: Texture? = null
    private var heartEmptyTexture: Texture? = null
    private var buttonNormalTexture: Texture? = null
    private var buttonPressedTexture: Texture? = null
    private lateinit var backgroundRegion: TextureRegion
    private val wallRegions = mutableListOf<TextureRegion>()
    private var wallAutotileTexture: Texture? = null
    private val wallAutotileRegions = mutableListOf<TextureRegion>()
    private lateinit var mouseRegion: TextureRegion
    private lateinit var cheeseRegion: TextureRegion
    private lateinit var catChaserRegion: TextureRegion
    private lateinit var catPatrolRegion: TextureRegion
    private lateinit var catRandomRegion: TextureRegion
    private lateinit var catAmbushRegion: TextureRegion
    private lateinit var heartFullRegion: TextureRegion
    private lateinit var heartEmptyRegion: TextureRegion
    private lateinit var inputHandler: InputHandler
    private lateinit var maze: MazeLoader
    private lateinit var mouse: Mouse
    private val cats = mutableListOf<Cat>()
    private val pellets = mutableListOf<Pellet>()
    // Стенные клетки, где вместо основного спрайта рисуется случайный доп темы
    private val extraWalls = mutableMapOf<GridPoint2, TextureRegion>()

    private var score = 0
    private var state = GameState.PLAYING
    private var lastCatcher: Cat? = null
    private val catchCount = mutableMapOf<String, Int>()
    private var isPaused = false
    private var lives = 1
    private var initialized = false

    // UI-оверлеи
    private var pauseContinueButton: UiButton? = null
    private var gameOverDialog: UiMenuOverlay? = null
    private var winDialog: UiMenuOverlay? = null
    private var exitDialog: UiMenuOverlay? = null
    private var activeDialog: UiOverlay? = null

    // Текущая тема мира/уровня (перезагружается при смене мира) и системный фон для оверлеев
    private lateinit var theme: ThemeLoader
    private var currentThemeId: String? = null
    private var systemBackgroundTexture: Texture? = null
    private lateinit var systemBackgroundRegion: TextureRegion

    private var currentLevel = catalog.indexOfMap(debugStartMap)

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
        font = generateFont(Constants.FONT_SIZE_HUD)
        hudFont = font
        menuTitleFont = generateFont(Constants.FONT_SIZE_TITLE)
        buttonFont = generateFont(Constants.FONT_SIZE_BUTTON)
        inputHandler = InputHandler()
        Gdx.input.inputProcessor = inputHandler
        Gdx.input.setCatchKey(com.badlogic.gdx.Input.Keys.BACK, true)

        // Системный фон (оверлеи), тема текущего уровня и статические спрайты
        loadSystemBackground()
        applyLevelTheme()
        loadStaticTextures()

        buildPauseButton()

        if (!initialized) {
            initialized = true
            startNewGame(keepScore = true)
            score = initialScore
            buildDialogs()
        }
    }

    /** Системный фон темы для оверлеев (Game Over / Победа / Выходим?). */
    private fun loadSystemBackground() {
        systemBackgroundTexture?.dispose()
        systemBackgroundTexture = Texture(Gdx.files.internal(systemTheme.backgroundPath)).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        systemBackgroundRegion = TextureRegion(systemBackgroundTexture)
    }

    /**
     * Тема текущего уровня: из каталога (или форс `--theme`). При смене мира
     * старые текстуры фона/стен освобождаются, новые загружаются.
     */
    private fun applyLevelTheme() {
        val id = themeOverride ?: catalog.themeFor(currentLevel)
        if (id == currentThemeId) return

        // Освобождаем текстуры предыдущей темы уровня
        backgroundTexture?.dispose()
        backgroundTexture = null
        wallTextures.forEach { it.dispose() }
        wallTextures.clear()
        wallRegions.clear()
        wallAutotileTexture?.dispose()
        wallAutotileTexture = null
        wallAutotileRegions.clear()
        extraWalls.clear()

        theme = ThemeLoader.forId(id)
        currentThemeId = theme.activeTheme
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

        Gdx.app.log(
            "GameScreen",
            "theme applied: ${theme.activeTheme} (level ${currentLevel + 1}/${catalog.count})"
        )
    }

    /** Спрайты персонажей и кнопок — не зависят от темы. */
    private fun loadStaticTextures() {
        mouseTexture = Texture(Gdx.files.internal("sprites/mouse.png"))
        cheeseTexture = Texture(Gdx.files.internal("sprites/cheese.png"))
        catChaserTexture = Texture(Gdx.files.internal("sprites/cat_chaser.png"))
        catPatrolTexture = Texture(Gdx.files.internal("sprites/cat_patrol.png"))
        catRandomTexture = Texture(Gdx.files.internal("sprites/cat_random.png"))
        catAmbushTexture = Texture(Gdx.files.internal("sprites/cat_ambush.png"))
        heartFullTexture = Texture(Gdx.files.internal("sprites/heart_full.png"))
        heartEmptyTexture = Texture(Gdx.files.internal("sprites/heart_empty.png"))
        buttonNormalTexture = Texture(Gdx.files.internal("ui/button.png"))
        buttonPressedTexture = Texture(Gdx.files.internal("ui/button_pressed.png"))

        val nearest = Texture.TextureFilter.Nearest
        mouseTexture!!.setFilter(nearest, nearest)
        cheeseTexture!!.setFilter(nearest, nearest)
        catChaserTexture!!.setFilter(nearest, nearest)
        catPatrolTexture!!.setFilter(nearest, nearest)
        catRandomTexture!!.setFilter(nearest, nearest)
        catAmbushTexture!!.setFilter(nearest, nearest)
        heartFullTexture!!.setFilter(nearest, nearest)
        heartEmptyTexture!!.setFilter(nearest, nearest)
        buttonNormalTexture!!.setFilter(nearest, nearest)
        buttonPressedTexture!!.setFilter(nearest, nearest)

        mouseRegion = TextureRegion(mouseTexture)
        cheeseRegion = TextureRegion(cheeseTexture)
        catChaserRegion = TextureRegion(catChaserTexture)
        catPatrolRegion = TextureRegion(catPatrolTexture)
        catRandomRegion = TextureRegion(catRandomTexture)
        catAmbushRegion = TextureRegion(catAmbushTexture)
        heartFullRegion = TextureRegion(heartFullTexture)
        heartEmptyRegion = TextureRegion(heartEmptyTexture)
    }

    private fun buildPauseButton() {
        pauseContinueButton = UiButton(
            bounds = Rectangle(
                Constants.WORLD_WIDTH / 2f - Constants.BUTTON_WIDTH / 2f,
                Constants.WORLD_HEIGHT / 2f - Constants.BUTTON_HEIGHT / 2f,
                Constants.BUTTON_WIDTH,
                Constants.BUTTON_HEIGHT
            ),
            text = MenuDefinitions.BTN_PAUSE_CONTINUE,
            font = buttonFont,
            normalTexture = buttonNormalTexture,
            pressedTexture = buttonPressedTexture,
            onClick = {
                Gdx.app.log("GameScreen", "pause continue clicked")
                pauseContinueButton?.isPressed = false
                isPaused = false
            }
        )
    }

    private fun generateFont(size: Int): BitmapFont {
        val fontFile = Gdx.files.internal("fonts/boldpixels.ttf")
        val generator = FreeTypeFontGenerator(fontFile)
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = size
        // Тёмно-коричневая обводка для контраста (кратно 16 — чёткая)
        parameter.borderWidth = size / 16f
        parameter.borderColor = Constants.FONT_COLOR_OUTLINE
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS +
            "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюя" +
            "0123456789:/% "
        val generatedFont = generator.generateFont(parameter)
        generator.dispose()
        return generatedFont
    }

    private fun buildDialogs() {
        // Мышка попалась! — полноэкранный оверлей в стиле главного меню (MENU.md §3.2)
        gameOverDialog = UiMenuOverlay(
            title = MenuDefinitions.TITLE_GAME_OVER,
            buttons = makeMenuButtons(MenuDefinitions.gameOver()) { action ->
                when (action) {
                    MenuDefinitions.Action.RESTART_LEVEL -> {
                        closeDialog()
                        restartCurrentLevel()
                    }
                    MenuDefinitions.Action.TO_MENU -> {
                        closeDialog()
                        mainGame.router.toMenu()
                    }
                    else -> {}
                }
            },
            titleFont = menuTitleFont,
            backgroundRegion = systemBackgroundRegion,
            backgroundCell = systemBackgroundRegion.regionWidth.toFloat()
        )

        // Победа (MENU.md §3.3) — как главное меню; зависит от текущей карты,
        // поэтому пересобирается при смене уровня
        val isFinal = catalog.isFinal(currentLevel)
        winDialog = UiMenuOverlay(
            title = if (isFinal) MenuDefinitions.TITLE_FINAL else MenuDefinitions.TITLE_WIN,
            buttons = makeMenuButtons(MenuDefinitions.win(isFinal)) { action ->
                when (action) {
                    MenuDefinitions.Action.NEXT_LEVEL -> {
                        closeDialog()
                        advanceToNextLevel()
                    }
                    MenuDefinitions.Action.TO_MENU -> {
                        closeDialog()
                        mainGame.progress.clear()
                        mainGame.router.toMenu()
                    }
                    else -> {}
                }
            },
            titleFont = menuTitleFont,
            backgroundRegion = systemBackgroundRegion,
            backgroundCell = systemBackgroundRegion.regionWidth.toFloat()
        )

        // Выходим? (MENU.md §3.4) — как главное меню
        exitDialog = UiMenuOverlay(
            title = MenuDefinitions.TITLE_EXIT,
            buttons = makeMenuButtons(MenuDefinitions.exitConfirm()) { action ->
                when (action) {
                    MenuDefinitions.Action.CANCEL -> closeDialog()
                    MenuDefinitions.Action.QUIT_APP -> {
                        closeDialog()
                        mainGame.router.quit()
                    }
                    else -> {}
                }
            },
            titleFont = menuTitleFont,
            backgroundRegion = systemBackgroundRegion,
            backgroundCell = systemBackgroundRegion.regionWidth.toFloat()
        )
    }

    /** Кнопки в стиле главного меню: группа по центру, зазор 7 px, ширина 360. */
    private fun makeMenuButtons(
        specs: List<MenuDefinitions.ButtonSpec>,
        onAction: (MenuDefinitions.Action) -> Unit
    ): List<UiButton> {
        val spacing = 7f
        val totalHeight = specs.size * Constants.BUTTON_HEIGHT + (specs.size - 1) * spacing
        val startY = Constants.WORLD_HEIGHT / 2f + totalHeight / 2f - Constants.BUTTON_HEIGHT
        return specs.mapIndexed { index, spec ->
            UiButton(
                bounds = Rectangle(
                    Constants.WORLD_WIDTH / 2f - Constants.BUTTON_WIDTH / 2f,
                    startY - index * (Constants.BUTTON_HEIGHT + spacing),
                    Constants.BUTTON_WIDTH,
                    Constants.BUTTON_HEIGHT
                ),
                text = spec.label,
                font = buttonFont,
                normalTexture = buttonNormalTexture,
                pressedTexture = buttonPressedTexture,
                onClick = { onAction(spec.action) }
            )
        }
    }

    private fun closeDialog() {
        activeDialog = null
    }

    private fun openDialog(dialog: UiOverlay?) {
        activeDialog = dialog
    }

    /** Перезапуск текущей карты (кнопка «Попробуй ещё!»). */
    private fun restartCurrentLevel() {
        startNewGame(keepScore = true)
    }

    /** Переход на следующую карту (кнопка «Следующая карта»). */
    private fun advanceToNextLevel() {
        currentLevel++
        startNewGame(keepScore = true)
        mainGame.progress.saveOnAdvance(currentLevel, score)
        buildDialogs()
    }

    private fun startNewGame(keepScore: Boolean = false) {
        inputHandler.reset()
        applyLevelTheme()
        Gdx.app.log("Game", "Loading map ${currentLevel + 1}/${catalog.count}: ${catalog.mapName(currentLevel)}")
        maze = MazeLoader(catalog.mapName(currentLevel).let { "maps/$it.txt" })
        setupExtraWalls()
        val mouseStart = maze.findMouseStart()
        mouse = Mouse(maze, mouseStart.x, mouseStart.y)
        cats.clear()

        val isFinal = catalog.isFinal(currentLevel)
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
        state = GameState.PLAYING
        isPaused = false
        pauseContinueButton?.isPressed = false
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
            // сыр появляется там, откуда стартуют коты (коты уходят
            // со старта в первый кадр, поэтому клетки не считаются занятыми)
            for (cell in catStartCells) {
                if (reachable.contains(cell)) {
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

        handleInput()

        when (state) {
            GameState.PLAYING -> {
                if (!isPaused && activeDialog == null) {
                    updatePlaying(delta)
                }
            }
            GameState.GAME_OVER -> {
                if (activeDialog == null) {
                    updateGameOver()
                }
            }
            GameState.WIN -> {
                if (activeDialog == null) {
                    updateWin()
                }
            }
        }

        // 1) Фон и стены через SpriteBatch
        spriteBatch.projectionMatrix = camera.combined
        drawBackgroundAndWalls()

        // 2) Сыр, мышь и коты
        drawPellets()
        drawEntities()

        drawHud()

        // Оверлей паузы
        if (isPaused) {
            drawPauseOverlay()
        }

        // Оверлей диалога
        activeDialog?.draw(spriteBatch, shapeRenderer)
    }

    private fun drawPauseOverlay() {
        // Затемнение экрана на 25 %
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.25f)
        shapeRenderer.rect(0f, 0f, Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)
        shapeRenderer.end()

        // Кнопка "Продолжить": на паузе она активна (Space/Enter/тап) — приподнята;
        // при нажатии/таче отжимается на место.
        spriteBatch.begin()
        pauseContinueButton?.isFocused = true
        pauseContinueButton?.draw(spriteBatch)
        spriteBatch.end()
    }

    private fun handleInput() {
        // --- ПАУЗА: любое разблокирующее действие = нажатие кнопки "Продолжить" ---
        if (isPaused) {
            // ESC, Space, Enter — программное нажатие кнопки
            // isPauseJustPressed() покрывает Space и pause-запрос от AI/MCP toggle
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE) ||
                Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER) ||
                inputHandler.isPauseJustPressed()
            ) {
                Gdx.app.log("GameScreen", "pause key pressed (ESC/Space/Enter)")
                pauseContinueButton?.triggerClick()
                return
            }

            // Тач/клик только в области кнопки
            if (Gdx.input.justTouched()) {
                val (touchX, touchY) = unprojectTouch()
                pauseContinueButton?.touchDown(touchX, touchY)
                inputHandler.cancelTouch()
                return
            }
            if (!Gdx.input.isTouched(0)) {
                val (touchX, touchY) = unprojectTouch()
                pauseContinueButton?.touchUp(touchX, touchY)
                inputHandler.cancelTouch()
                return
            }
            return
        }

        // --- Игра не на паузе ---

        // ESC — диалог "Выйти?" (не подменяет уже открытый оверлей)
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            Gdx.app.log("GameScreen", "ESC pressed, activeDialog=$activeDialog")
            when {
                activeDialog == exitDialog -> closeDialog()
                activeDialog == null -> {
                    exitDialog?.focusedIndex = 0
                    openDialog(exitDialog)
                }
            }
            return
        }

        // Android BACK — пауза в игре, выход в меню из диалогов
        if (Gdx.app.type == Application.ApplicationType.Android &&
            Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.BACK)
        ) {
            Gdx.app.log("GameScreen", "BACK pressed")
            when {
                activeDialog != null -> closeDialog()
                state == GameState.PLAYING -> isPaused = true
                else -> mainGame.router.toMenu()
            }
            return
        }

        // Space — поставить паузу
        if (state == GameState.PLAYING && activeDialog == null && inputHandler.isPauseJustPressed()) {
            Gdx.app.log("GameScreen", "Space pressed — pause")
            isPaused = true
            return
        }

        // Оверлей-меню (Game Over): активная кнопка, навигация и активация с клавиатуры
        if (activeDialog is UiMenuOverlay) {
            val overlay = activeDialog as UiMenuOverlay
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)) overlay.moveFocus(-1)
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)) overlay.moveFocus(1)
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)
            ) {
                overlay.triggerFocused()
                return
            }
        }

        // Тач: tap vs swipe для активного диалога (Game Over / Win / Выйти?)
        if (activeDialog != null) {
            if (Gdx.input.justTouched()) {
                val (touchX, touchY) = unprojectTouch()
                activeDialog?.touchDown(touchX, touchY)
                inputHandler.cancelTouch()
            }
            if (!Gdx.input.isTouched(0)) {
                val (touchX, touchY) = unprojectTouch()
                activeDialog?.touchUp(touchX, touchY)
                inputHandler.cancelTouch()
            }
        }
    }

    private fun unprojectTouch(): Pair<Float, Float> {
        val touchPos = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        viewport.unproject(touchPos)
        return touchPos.x to touchPos.y
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
            Gdx.app.log("GameScreen", "WIN, score=$score")
        }
    }

    private fun checkCatCollision(): Boolean {
        for (cat in cats) {
            if (cat.cell == mouse.cell) {
                state = GameState.GAME_OVER
                Gdx.app.log("GameScreen", "GAME_OVER, catcher=${cat.name}")
                lastCatcher = cat
                catchCount[cat.name] = catchCount.getOrDefault(cat.name, 0) + 1
                return true
            }
        }
        return false
    }

    private fun updateGameOver() {
        // «Продолжить» вернёт на карту гибели (MENU.md §4)
        mainGame.progress.saveOnDeath(currentLevel, score)
        // Активная кнопка по умолчанию — верхняя
        gameOverDialog?.focusedIndex = 0
        openDialog(gameOverDialog)
    }

    private fun updateWin() {
        // Победа на не-финальной карте готовит продолжение со следующей (MENU.md §4)
        if (!catalog.isFinal(currentLevel)) {
            mainGame.progress.saveOnAdvance(currentLevel + 1, score)
        }
        // Активная кнопка по умолчанию — верхняя
        winDialog?.focusedIndex = 0
        openDialog(winDialog)
    }

    private fun setupExtraWalls() {
        extraWalls.clear()
        if (theme.hasAutotile) return
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

        drawBackground()
        if (theme.hasAutotile) {
            drawAutotileWalls()
        } else {
            drawClassicWalls()
        }

        spriteBatch.end()
    }

    /**
     * Фон — тайлы по всему экрану (за лабиринтом остаётся фон, а не чёрная заливка).
     * Тайл рисуется 1:1 в натуральном размере текстуры (например, 200×200),
     * поэтому узор повторяется крупно и стыкуется без зазоров.
     */
    private fun drawBackground() {
        val tileW = backgroundRegion.regionWidth.toFloat()
        val tileH = backgroundRegion.regionHeight.toFloat()
        val stepW = tileW.toInt()
        val stepH = tileH.toInt()
        for (x in 0 until Constants.WORLD_WIDTH.toInt() + stepW step stepW) {
            for (y in 0 until Constants.WORLD_HEIGHT.toInt() + stepH step stepH) {
                spriteBatch.draw(backgroundRegion, x.toFloat(), y.toFloat(), tileW, tileH)
            }
        }
    }

    /** Классический рендер: один спрайт на каждую клетку стены + случайные допы. */
    private fun drawClassicWalls() {
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
    }

    /** Dual-grid 15-piece рендер: тайл рисуется в каждом углу между логическими клетками. */
    private fun drawAutotileWalls() {
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
                spriteBatch.draw(region, x, y, Constants.CELL_SIZE, Constants.CELL_SIZE)
                }
            }
        }
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

        // Плашки HUD нет: фон темы залит по всему экрану (как в меню/заставке),
        // текст и сердечки рисуются прямо поверх него.
        spriteBatch.begin()
        hudFont.color.set(Constants.FONT_COLOR_TEXT)
        // Счёт и рекорд временно скрыты (счёт не ведём)

        // Центр: сыр и карта
        val pelletsText = "Сыр: $eatenCount/${pellets.size}"
        val mapText = "Карта ${currentLevel + 1}/${catalog.count}"
        val pelletsLayout = GlyphLayout(hudFont, pelletsText)
        val mapLayout = GlyphLayout(hudFont, mapText)
        hudFont.draw(spriteBatch, pelletsText, Constants.WORLD_WIDTH / 2f - pelletsLayout.width / 2f, Constants.WORLD_HEIGHT - 14f)
        hudFont.draw(spriteBatch, mapText, Constants.WORLD_WIDTH / 2f - mapLayout.width / 2f, Constants.WORLD_HEIGHT - 38f)

        // Справа: сердечки жизней
        val heartsStartX = Constants.WORLD_WIDTH - 20f - Constants.MAX_LIVES * Constants.HEART_SIZE
        val heartY = Constants.WORLD_HEIGHT - Constants.HUD_HEIGHT + 14f
        for (i in 0 until Constants.MAX_LIVES) {
            val region = if (i < lives) heartFullRegion else heartEmptyRegion
            spriteBatch.draw(
                region,
                heartsStartX + i * Constants.HEART_SIZE,
                heartY,
                Constants.HEART_SIZE,
                Constants.HEART_SIZE
            )
        }

        // Легенда котов внизу слева
        val legendStartY = 120f
        cats.forEachIndexed { index, cat ->
            val legendText = "${cat.name}: ${catchCount[cat.name] ?: 0}"
            hudFont.color = cat.color
            hudFont.draw(spriteBatch, legendText, 20f, legendStartY - index * 24f)
        }
        hudFont.color.set(Constants.FONT_COLOR_TEXT)

        // Версия в правом нижнем углу (с режимом запуска)
        val versionText = "v${Constants.VERSION}${LaunchMode.suffix}"
        val versionLayout = GlyphLayout(hudFont, versionText)
        hudFont.draw(spriteBatch, versionText, Constants.WORLD_WIDTH - versionLayout.width - 20f, 30f)
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
        wallAutotileTexture?.dispose()
        systemBackgroundTexture?.dispose()
        mouseTexture?.dispose()
        cheeseTexture?.dispose()
        catChaserTexture?.dispose()
        catPatrolTexture?.dispose()
        catRandomTexture?.dispose()
        catAmbushTexture?.dispose()
        heartFullTexture?.dispose()
        heartEmptyTexture?.dispose()
        buttonNormalTexture?.dispose()
        buttonPressedTexture?.dispose()
        if (::font.isInitialized) font.dispose()
        if (::menuTitleFont.isInitialized) menuTitleFont.dispose()
        if (::buttonFont.isInitialized) buttonFont.dispose()
        Gdx.input.inputProcessor = null
    }

    //region AI-отладка: публичные методы состояния и управления

    fun getGameState(): String = when {
        state == GameState.PLAYING && isPaused -> "PAUSED"
        state == GameState.PLAYING -> "PLAYING"
        state == GameState.GAME_OVER -> "GAME_OVER"
        state == GameState.WIN -> "VICTORY"
        else -> "PLAYING"
    }

    fun getMouseCell(): Pair<Int, Int> = mouse.cell.x to mouse.cell.y

    fun getMouseDirection(): Direction = mouse.direction

    fun getCatsInfo(): List<Map<String, Any>> = cats.map {
        mapOf(
            "name" to it.name,
            "type" to when (it.strategy) {
                CatStrategy.Chaser -> "Chaser"
                CatStrategy.Patrol -> "Patrol"
                CatStrategy.Random -> "Random"
                CatStrategy.Ambush -> "Ambush"
            },
            "x" to it.cell.x,
            "y" to it.cell.y
        )
    }

    fun getRemainingCheese(): List<Pair<Int, Int>> =
        pellets.filter { !it.eaten }.map { it.cell.x to it.cell.y }

    fun getTotalCheeseCount(): Int = pellets.size

    fun getCollectedCheeseCount(): Int = pellets.count { it.eaten }

    fun getScore(): Int = score

    fun getLives(): Int = 1

    fun getCurrentMapName(): String =
        catalog.mapName(currentLevel)

    fun resetLevel() {
        startNewGame()
    }

    fun setAiDirection(direction: Direction) {
        inputHandler.setDirection(direction)
    }

    fun toggleAiPause() {
        inputHandler.togglePause()
    }

    //endregion

    private enum class GameState {
        PLAYING, GAME_OVER, WIN
    }
}
