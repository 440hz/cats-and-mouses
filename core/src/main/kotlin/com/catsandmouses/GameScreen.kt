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
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport
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
    /** Playtest-режим редактора: карта берётся из текста, а не из файла. */
    private val overrideMazeText: String? = null,
    /** Playtest-режим: не писать прогресс в Preferences. */
    private val saveProgress: Boolean = true,
    /** Playtest-режим: ESC вызывает этот колбэк вместо диалога «Выходим?». */
    private val onExitOverride: (() -> Unit)? = null,
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

    /** Игровая симуляция (мышь, коты, сыр, счёт, состояние) — без рендера. */
    private val world = GameWorld()

    private val mouse: Mouse get() = world.mouse
    private val cats: MutableList<Cat> get() = world.cats
    private val pellets: MutableList<Pellet> get() = world.pellets
    private val catchCount: MutableMap<String, Int> get() = world.catchCount
    private val state: GameWorld.State get() = world.state

    /** Отрисовка фона и стен лабиринта (общий код с редактором карт). */
    private val mazeRenderer = MazeRenderer()

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
            world.setScore(initialScore)
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

        theme = ThemeLoader.forId(id)
        currentThemeId = theme.activeTheme
        mazeRenderer.loadTheme(theme)

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
        if (saveProgress) mainGame.progress.saveOnAdvance(currentLevel, world.score)
        buildDialogs()
    }

    private fun startNewGame(keepScore: Boolean = false) {
        inputHandler.reset()
        applyLevelTheme()
        val mapName = catalog.mapName(currentLevel)
        val draft = overrideMazeText
        Gdx.app.log(
            "Game",
            "Loading map ${currentLevel + 1}/${catalog.count}: $mapName${if (draft != null) " (draft)" else ""}"
        )
        maze = if (draft != null) MazeLoader.fromText(draft) else MazeLoader("maps/$mapName.txt")
        mazeRenderer.setupExtraWalls(theme, maze)
        world.startLevel(
            maze = maze,
            mouseStart = maze.findMouseStart(),
            isFinal = catalog.isFinal(currentLevel),
            keepScore = keepScore
        )
        isPaused = false
        pauseContinueButton?.isPressed = false
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        handleInput()

        when (state) {
            GameWorld.State.PLAYING -> {
                if (!isPaused && activeDialog == null) {
                    world.update(delta, inputHandler.getDirection())
                }
            }
            GameWorld.State.GAME_OVER -> {
                if (activeDialog == null) {
                    updateGameOver()
                }
            }
            GameWorld.State.WIN -> {
                if (activeDialog == null) {
                    updateWin()
                }
            }
        }

        // 1) Фон и стены через SpriteBatch
        spriteBatch.projectionMatrix = camera.combined
        drawBackgroundAndWalls()

        // 2) Сыр, сердечки, мышь и коты
        drawPellets()
        drawHearts()
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

        // ESC — диалог "Выйти?" (не подменяет уже открытый оверлей).
        // В playtest-режиме ESC сразу возвращает в редактор (onExitOverride).
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            if (onExitOverride != null) {
                Gdx.app.log("GameScreen", "ESC pressed — return to editor")
                onExitOverride.invoke()
                return
            }
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
                state == GameWorld.State.PLAYING -> isPaused = true
                else -> mainGame.router.toMenu()
            }
            return
        }

        // Space — поставить паузу
        if (state == GameWorld.State.PLAYING && activeDialog == null && inputHandler.isPauseJustPressed()) {
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

    private fun updateGameOver() {
        // «Продолжить» вернёт на карту гибели (MENU.md §4); в playtest прогресс не пишем
        if (saveProgress) mainGame.progress.saveOnDeath(currentLevel, world.score)
        // Активная кнопка по умолчанию — верхняя
        gameOverDialog?.focusedIndex = 0
        openDialog(gameOverDialog)
    }

    private fun updateWin() {
        // Победа на не-финальной карте готовит продолжение со следующей (MENU.md §4)
        if (saveProgress && !catalog.isFinal(currentLevel)) {
            mainGame.progress.saveOnAdvance(currentLevel + 1, world.score)
        }
        // Активная кнопка по умолчанию — верхняя
        winDialog?.focusedIndex = 0
        openDialog(winDialog)
    }

    private fun drawBackgroundAndWalls() {
        spriteBatch.begin()
        mazeRenderer.drawBackground(spriteBatch)
        mazeRenderer.drawWalls(spriteBatch, maze)
        spriteBatch.end()
    }

    private fun drawPellets() {
        spriteBatch.begin()
        for (pellet in pellets) {
            pellet.draw(spriteBatch, cheeseRegion)
        }
        spriteBatch.end()
    }

    private fun drawHearts() {
        spriteBatch.begin()
        for (heart in world.hearts) {
            heart.draw(spriteBatch, heartFullRegion)
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
        mazeRenderer.dispose()
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
        state == GameWorld.State.PLAYING && isPaused -> "PAUSED"
        state == GameWorld.State.PLAYING -> "PLAYING"
        state == GameWorld.State.GAME_OVER -> "GAME_OVER"
        state == GameWorld.State.WIN -> "VICTORY"
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

    fun getScore(): Int = world.score

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
}
