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
import com.catsandmouses.entities.Cat
import com.catsandmouses.entities.CatStrategy
import com.catsandmouses.entities.Mouse
import com.catsandmouses.entities.Pellet
import com.catsandmouses.audio.AudioManager
import com.catsandmouses.graphics.Celebration
import com.catsandmouses.graphics.CheeseAnimation
import com.catsandmouses.graphics.ContinuousAnimator
import com.catsandmouses.graphics.HeartAnimation
import com.catsandmouses.graphics.OccasionalAnimator
import com.catsandmouses.i18n.I18n
import com.catsandmouses.i18n.Str
import kotlin.random.Random

class GameScreen(
    private val systemTheme: ThemeLoader,
    private val catalog: LevelCatalog,
    private val themeOverride: String? = null,
    private val debugStartMap: String = "1/1",
    private val debugLogs: Boolean = false,
    private val initialScore: Int = 0,
    /** Стартовый запас жизней (из прогресса/меню). */
    private val initialLives: Int = Constants.START_LIVES,
    /** Playtest-режим редактора: карта берётся из текста, а не из файла. */
    private val overrideMazeText: String? = null,
    /** Playtest-режим: не писать прогресс в Preferences. */
    private val saveProgress: Boolean = true,
    /** Playtest-режим: ESC вызывает этот колбэк вместо диалога «Выходим?». */
    private val onExitOverride: (() -> Unit)? = null,
    /** Показывать стартовый отсчёт «3 → 2 → 1» (в AI-режиме и playtest — выключен). */
    private val countdownEnabled: Boolean = true,
    private val mainGame: MainGame
) : Screen {

    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var hudFont: BitmapFont
    private lateinit var countdownFont: BitmapFont
    private lateinit var menuTitleFont: BitmapFont
    private lateinit var buttonFont: BitmapFont
    private var mouseTexture: Texture? = null
    private var cheeseTexture: Texture? = null
    private var catChaserTexture: Texture? = null
    private var catPatrolTexture: Texture? = null
    private var catRandomTexture: Texture? = null
    private var catAmbushTexture: Texture? = null
    private var heartFullTexture: Texture? = null
    private var heartGreyTexture: Texture? = null
    private var buttonNormalTexture: Texture? = null
    private var buttonPressedTexture: Texture? = null
    private lateinit var mouseRegion: TextureRegion
    private lateinit var cheeseRegion: TextureRegion
    private lateinit var catChaserRegion: TextureRegion
    private lateinit var catPatrolRegion: TextureRegion
    private lateinit var catRandomRegion: TextureRegion
    private lateinit var catAmbushRegion: TextureRegion
    private lateinit var heartFullRegion: TextureRegion
    private lateinit var heartGreyRegion: TextureRegion
    private lateinit var inputHandler: InputHandler
    private lateinit var maze: MazeLoader

    /** Игровая симуляция (мышь, коты, сыр, счёт, состояние) — без рендера. */
    private val world = GameWorld()

    private val mouse: Mouse get() = world.mouse
    private val cats: MutableList<Cat> get() = world.cats
    private val pellets: MutableList<Pellet> get() = world.pellets
    private val state: GameWorld.State get() = world.state

    /** Отрисовка фона и стен лабиринта (общий код с редактором карт). */
    private val mazeRenderer = MazeRenderer()

    private var isPaused = false
    private var initialized = false

    /** Idle-аниматоры сыра (по клетке): изредка проигрывают случайный акцент. */
    private val cheeseAnimators = HashMap<GridPoint2, OccasionalAnimator>()

    /** Аниматоры сердечек (по клетке): постоянный пульс масштабом. */
    private val heartAnimators = HashMap<GridPoint2, ContinuousAnimator>()

    /** Праздник после финала: салют и шарики поверх замороженной карты (MENU.md §3.3.1). */
    private val celebration = Celebration()
    private var celebrating = false

    /** Палитра праздника (салют/шарики) — фиксированные яркие цвета. */
    private val celebrationColors = listOf(
        Color(0.95f, 0.28f, 0.32f, 1f), // красный
        Color(1.00f, 0.80f, 0.20f, 1f), // жёлтый
        Color(0.35f, 0.85f, 0.40f, 1f), // зелёный
        Color(0.30f, 0.75f, 0.98f, 1f), // голубой
        Color(0.90f, 0.45f, 0.90f, 1f), // сиреневый
        Color(1.00f, 0.55f, 0.20f, 1f), // оранжевый
        Color(0.98f, 0.98f, 0.98f, 1f)  // белый
    )

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
        countdownFont = generateCountdownFont()
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
            startNewGame(keepScore = true, startLives = initialLives)
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
        heartGreyTexture = Texture(Gdx.files.internal("sprites/heart_grey.png"))
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
        heartGreyTexture!!.setFilter(nearest, nearest)
        buttonNormalTexture!!.setFilter(nearest, nearest)
        buttonPressedTexture!!.setFilter(nearest, nearest)

        mouseRegion = TextureRegion(mouseTexture)
        cheeseRegion = TextureRegion(cheeseTexture)
        catChaserRegion = TextureRegion(catChaserTexture)
        catPatrolRegion = TextureRegion(catPatrolTexture)
        catRandomRegion = TextureRegion(catRandomTexture)
        catAmbushRegion = TextureRegion(catAmbushTexture)
        heartFullRegion = TextureRegion(heartFullTexture)
        heartGreyRegion = TextureRegion(heartGreyTexture)
    }

    private fun buildPauseButton() {
        pauseContinueButton = UiButton(
            bounds = Rectangle(
                Constants.WORLD_WIDTH / 2f - Constants.BUTTON_WIDTH / 2f,
                Constants.WORLD_HEIGHT / 2f - Constants.BUTTON_HEIGHT / 2f,
                Constants.BUTTON_WIDTH,
                Constants.BUTTON_HEIGHT
            ),
            text = MenuDefinitions.btnPauseContinue(),
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

    /** Крупный шрифт только для цифр отсчёта (3/2/1) — маленький атлас. */
    private fun generateCountdownFont(): BitmapFont {
        val fontFile = Gdx.files.internal("fonts/boldpixels.ttf")
        val generator = FreeTypeFontGenerator(fontFile)
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = Constants.FONT_SIZE_COUNTDOWN
        parameter.borderWidth = Constants.FONT_SIZE_COUNTDOWN / 16f
        parameter.borderColor = Constants.FONT_COLOR_OUTLINE
        parameter.characters = "123"
        val generatedFont = generator.generateFont(parameter)
        generator.dispose()
        return generatedFont
    }

    private fun buildDialogs() {
        // Мышка попалась! — полноэкранный оверлей в стиле главного меню (MENU.md §3.2)
        gameOverDialog = UiMenuOverlay(
            title = MenuDefinitions.titleGameOver(),
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
            title = if (isFinal) MenuDefinitions.titleFinal() else MenuDefinitions.titleWin(),
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
            title = MenuDefinitions.titleExit(),
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

    /**
     * SFX по дельтам кадра — так симуляция [GameWorld] остаётся чистой (без Gdx).
     */
    private fun playGameplaySfx(prevScore: Int, prevLives: Int, prevHearts: Int, prevCountdown: Int) {
        if (world.score > prevScore) mainGame.audio.playSfx("cheese")
        if (world.hearts.count { it.collected } > prevHearts) mainGame.audio.playSfx("heart")
        if (world.lives < prevLives && world.state == GameWorld.State.PLAYING) {
            mainGame.audio.playSfx("life_lost")
        }
        if (world.isCountdown && world.countdownNumber != prevCountdown) {
            mainGame.audio.playSfx("tick")
        }
    }

    private fun openDialog(dialog: UiOverlay?) {
        activeDialog = dialog
    }

    /** Перезапуск текущей карты (кнопка «Попробуй ещё!»). Попытка начинается с 1 жизни. */
    private fun restartCurrentLevel() {
        startNewGame(keepScore = true, startLives = Constants.START_LIVES)
    }

    /** Переход на следующую карту (кнопка «Следующая карта»); жизни переносятся. */
    private fun advanceToNextLevel() {
        val carryLives = world.lives
        currentLevel++
        Gdx.app.log("Game", "advance to ${currentLevel + 1}/${catalog.count}: carryLives=$carryLives")
        startNewGame(keepScore = true, startLives = carryLives)
        if (saveProgress) mainGame.progress.saveOnAdvance(currentLevel, world.score, carryLives)
        buildDialogs()
    }

    private fun startNewGame(keepScore: Boolean = false, startLives: Int = Constants.START_LIVES) {
        inputHandler.reset()
        applyLevelTheme()
        val mapName = catalog.mapName(currentLevel)
        val draft = overrideMazeText
        Gdx.app.log(
            "Game",
            "Loading map ${currentLevel + 1}/${catalog.count}: $mapName" +
                "${if (draft != null) " (draft)" else ""} startLives=$startLives keepScore=$keepScore"
        )
        maze = if (draft != null) MazeLoader.fromText(draft) else MazeLoader("maps/$mapName.txt")
        mazeRenderer.setupExtraWalls(theme, maze)
        val mouseStart = maze.findMouseStartFacing()
        world.startLevel(
            maze = maze,
            mouseStart = mouseStart.cell,
            isFinal = catalog.isFinal(currentLevel),
            keepScore = keepScore,
            startLives = startLives,
            countdown = if (countdownEnabled) Constants.COUNTDOWN_TIME else 0f,
            mouseFacing = mouseStart.facing
        )
        isPaused = false
        pauseContinueButton?.isPressed = false
        celebrating = false
        celebration.reset()

        // Музыка геймплея (no-op, если тот же трек уже играет)
        mainGame.audio.playMusic(AudioManager.TRACK_GAMEPLAY)
        if (world.isCountdown) mainGame.audio.playSfx("tick")

        cheeseAnimators.clear()
        // Соль на запуск уровня: у каждого сыра своё расписание (по клетке),
        // но открытие меняется между перезапусками.
        val runSalt = Random.nextLong()
        for (pellet in world.pellets) {
            val cell = pellet.cell
            val seed = CheeseAnimation.cellSeed(cell.x, cell.y) xor runSalt
            cheeseAnimators[cell] = CheeseAnimation.create(seed)
        }

        // Сердечки пульсируют постоянно; фаза — по клетке (не в один такт).
        heartAnimators.clear()
        for (heart in world.hearts) {
            val cell = heart.cell
            heartAnimators[cell] = HeartAnimation.create(HeartAnimation.cellSeed(cell.x, cell.y))
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        handleInput()

        when (state) {
            GameWorld.State.PLAYING -> {
                if (!isPaused && activeDialog == null) {
                    val prevScore = world.score
                    val prevLives = world.lives
                    val prevHearts = world.hearts.count { it.collected }
                    val prevCountdown = world.countdownNumber
                    world.update(delta, inputHandler.getDirection())
                    playGameplaySfx(prevScore, prevLives, prevHearts, prevCountdown)
                    // Анимации идут только в живой игре (не на отсчёте/паузе/оверлее)
                    if (!world.isCountdown) {
                        cheeseAnimators.values.forEach { it.update(delta) }
                        heartAnimators.values.forEach { it.update(delta) }
                    }
                }
            }
            GameWorld.State.GAME_OVER -> {
                if (activeDialog == null) {
                    mainGame.audio.stopMusic()
                    mainGame.audio.playSfx("game_over")
                    updateGameOver()
                }
            }
            GameWorld.State.WIN -> {
                if (activeDialog == null) {
                    // Финал в реальной игре → праздник на карте (салют/шарики), затем титры.
                    // В playtest редактора оставляем прежний оверлей «Финал пройден!».
                    if (saveProgress && catalog.isFinal(currentLevel)) {
                        if (!celebrating) {
                            Gdx.app.log("GameScreen", "final win — celebration")
                            celebrating = true
                            celebration.reset()
                            mainGame.progress.clear()
                            mainGame.audio.stopMusic()
                            mainGame.audio.playSfx("win")
                        }
                    } else {
                        mainGame.audio.stopMusic()
                        mainGame.audio.playSfx("win")
                        updateWin()
                    }
                }
            }
        }

        // Праздник после финала: тикает поверх замороженной карты, затем → титры
        if (celebrating) {
            celebration.update(delta)
        }

        // Музыка: приглушение (ducking) на паузе
        mainGame.audio.setDuck(state == GameWorld.State.PLAYING && isPaused)

        // 1) Фон и стены через SpriteBatch
        spriteBatch.projectionMatrix = camera.combined
        drawBackgroundAndWalls()

        // 2) Сыр, сердечки, мышь и коты
        drawPellets()
        drawHearts()
        drawEntities()

        drawHud()

        // Стартовый отсчёт 3 → 2 → 1
        if (world.isCountdown) {
            drawCountdown()
        }

        // Оверлей паузы
        if (isPaused) {
            drawPauseOverlay()
        }

        // Оверлей диалога
        activeDialog?.draw(spriteBatch, shapeRenderer)

        // Праздник после финала: салют и шарики поверх карты; по завершении → титры
        if (celebrating) {
            drawCelebration()
            if (celebration.isFinished) {
                Gdx.app.log("GameScreen", "celebration done → credits")
                celebrating = false
                mainGame.router.showCredits()
                return
            }
        }
    }

    /** Праздник после финала (MENU.md §3.3.1): шарики снизу, салют, надпись «УРА!». */
    private fun drawCelebration() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // Плавное появление/затухание всего праздника у краёв по времени
        val p = celebration.progress
        val fade = when {
            p < 0.08f -> p / 0.08f
            p > 0.92f -> (1f - p) / 0.08f
            else -> 1f
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Шарики: нить + тело-эллипс
        for (b in celebration.balloons) {
            val color = celebrationColors[b.colorIndex % celebrationColors.size]
            val cx = b.x + celebration.sway(b)
            val radius = b.size / 2f
            val w = b.size * 0.86f
            val h = b.size
            shapeRenderer.setColor(color.r, color.g, color.b, fade)
            shapeRenderer.rect(cx - 1f, b.y - h, 2f, h * 0.7f)          // нить
            shapeRenderer.ellipse(cx - w / 2f, b.y - radius, w, h)      // тело
        }

        // Искры салюта
        for (s in celebration.sparks) {
            val color = celebrationColors[s.colorIndex % celebrationColors.size]
            val a = celebration.sparkAlpha(s) * fade
            shapeRenderer.setColor(color.r, color.g, color.b, a)
            shapeRenderer.circle(s.x, s.y, 3.5f, 8)
        }

        shapeRenderer.end()

        // Надпись по центру
        val text = I18n.t(Str.CELEBRATION)
        menuTitleFont.color.set(
            Constants.FONT_COLOR_TEXT.r,
            Constants.FONT_COLOR_TEXT.g,
            Constants.FONT_COLOR_TEXT.b,
            fade
        )
        val layout = GlyphLayout(menuTitleFont, text)
        spriteBatch.begin()
        menuTitleFont.draw(
            spriteBatch,
            layout,
            Constants.WORLD_WIDTH / 2f - layout.width / 2f,
            Constants.WORLD_HEIGHT * 0.66f
        )
        spriteBatch.end()
    }

    /**
     * Крупная цифра отсчёта по центру с «наплывом» и затуханием:
     * 0.00–0.15 с — появление (alpha 0→1, масштаб 1.15→1.00);
     * 0.15–0.60 с — удержание;
     * 0.60–1.00 с — затухание (alpha 1→0, масштаб 1.00→1.08).
     * Затемнение фона синхронно с цифрой.
     */
    private fun drawCountdown() {
        val t = (world.countdownNumber - world.countdownTimer).coerceIn(0f, 1f)
        val alpha: Float
        val scale: Float
        when {
            t < 0.15f -> {
                val p = t / 0.15f
                alpha = p
                scale = 1.15f - 0.15f * p
            }
            t < 0.60f -> {
                alpha = 1f
                scale = 1f
            }
            else -> {
                val p = (t - 0.60f) / 0.40f
                alpha = 1f - p
                scale = 1f + 0.08f * p
            }
        }

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.25f * alpha)
        shapeRenderer.rect(0f, 0f, Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)
        shapeRenderer.end()

        countdownFont.data.setScale(scale)
        val layout = GlyphLayout(countdownFont, world.countdownNumber.toString())
        spriteBatch.begin()
        countdownFont.color.set(
            Constants.FONT_COLOR_TEXT.r,
            Constants.FONT_COLOR_TEXT.g,
            Constants.FONT_COLOR_TEXT.b,
            alpha
        )
        countdownFont.draw(
            spriteBatch,
            layout,
            Constants.WORLD_WIDTH / 2f - layout.width / 2f,
            Constants.WORLD_HEIGHT / 2f + layout.height / 2f
        )
        spriteBatch.end()
        countdownFont.data.setScale(1f)
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
        // Во время праздника после финала ввод игнорируется (нельзя открыть паузу/«Выходим?»)
        if (celebrating) return

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

        // Стартовый отсчёт: пауза/меню недоступны, ввод только буферизуется
        if (world.isCountdown) return

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
        // Победа на не-финальной карте готовит продолжение со следующей (MENU.md §4);
        // жизни переносятся (иначе «Продолжить» после победы стартует со START_LIVES)
        if (saveProgress && !catalog.isFinal(currentLevel)) {
            mainGame.progress.saveOnAdvance(currentLevel + 1, world.score, world.lives)
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
            val transform = cheeseAnimators[pellet.cell]?.transform()
            if (transform == null) {
                pellet.draw(spriteBatch, cheeseRegion)
            } else {
                pellet.draw(
                    spriteBatch,
                    cheeseRegion,
                    scale = transform.scale,
                    rotation = transform.rotation,
                    offsetY = transform.offsetY
                )
            }
        }
        spriteBatch.end()
    }

    private fun drawHearts() {
        spriteBatch.begin()
        for (heart in world.hearts) {
            val scale = heartAnimators[heart.cell]?.transform()?.scale ?: 1f
            heart.draw(spriteBatch, heartFullRegion, scale)
        }
        spriteBatch.end()
    }

    private fun drawEntities() {
        // Мышь и коты — спрайтами с поворотом по направлению.
        // После потери жизни мышь мигает (~10 Гц) на время неуязвимости.
        val mouseVisible = !world.isInvulnerable || (world.invulnTimer * 10f).toInt() % 2 == 0
        spriteBatch.begin()
        if (mouseVisible) {
            mouse.draw(spriteBatch, mouseRegion)
        }
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
        val pelletsText = I18n.t(Str.HUD_CHEESE, eatenCount, pellets.size)
        val mapText = I18n.t(Str.HUD_MAP, currentLevel + 1, catalog.count)
        val pelletsLayout = GlyphLayout(hudFont, pelletsText)
        val mapLayout = GlyphLayout(hudFont, mapText)
        hudFont.draw(spriteBatch, pelletsText, Constants.WORLD_WIDTH / 2f - pelletsLayout.width / 2f, Constants.WORLD_HEIGHT - 14f)
        hudFont.draw(spriteBatch, mapText, Constants.WORLD_WIDTH / 2f - mapLayout.width / 2f, Constants.WORLD_HEIGHT - 38f)

        // Справа: сердечки жизней (7 слотов уменьшенного размера HUD_HEART_SIZE)
        val slot = Constants.HUD_HEART_SIZE
        val heartsStartX = Constants.WORLD_WIDTH - 20f - Constants.MAX_LIVES * slot
        val heartY = Constants.WORLD_HEIGHT - Constants.HUD_HEIGHT + 14f
        for (i in 0 until Constants.MAX_LIVES) {
            val region = if (i < world.lives) heartFullRegion else heartGreyRegion
            spriteBatch.draw(region, heartsStartX + i * slot, heartY, slot, slot)
        }

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
        heartGreyTexture?.dispose()
        buttonNormalTexture?.dispose()
        buttonPressedTexture?.dispose()
        if (::font.isInitialized) font.dispose()
        if (::countdownFont.isInitialized) countdownFont.dispose()
        if (::menuTitleFont.isInitialized) menuTitleFont.dispose()
        if (::buttonFont.isInitialized) buttonFont.dispose()
        Gdx.input.inputProcessor = null
    }

    //region AI-отладка: публичные методы состояния и управления

    fun getGameState(): String = when {
        state == GameWorld.State.PLAYING && world.isCountdown -> "COUNTDOWN"
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

    fun getLives(): Int = world.lives

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
