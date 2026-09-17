package com.catsandmouses

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport

/**
 * Главное меню: фон темы, титул, рекорд, кнопки.
 */
class MainMenuScreen(
    private val preferences: Preferences,
    private val onStartGame: () -> Unit,
    private val onContinueGame: () -> Unit,
    private val onExit: () -> Unit
) : Screen {

    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var batch: SpriteBatch
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var titleFont: BitmapFont
    private lateinit var buttonFont: BitmapFont
    private lateinit var versionFont: BitmapFont
    private var bgTexture: Texture? = null
    private var buttonNormalTexture: Texture? = null
    private var buttonPressedTexture: Texture? = null

    private val buttons = mutableListOf<UiButton>()
    private var focusedIndex = 0
    private var hasProgress = false
    private var pointerDown = false
    private var pressedButtonIndex = -1
    // Защита от "протёкшего" нажатия: клавиша, которой пролистнули заставку,
    // живёт до конца кадра, поэтому первые кадры клавиатура в меню игнорируется.
    private var skipKeyboardFrames = 2

    override fun show() {
        camera = OrthographicCamera(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)
        camera.position.set(Constants.WORLD_WIDTH / 2f, Constants.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        viewport = FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, camera)
        batch = SpriteBatch()
        batch.projectionMatrix = camera.combined
        shapeRenderer = ShapeRenderer()
        shapeRenderer.projectionMatrix = camera.combined

        titleFont = generateFont(Constants.FONT_SIZE_TITLE)
        buttonFont = generateFont(Constants.FONT_SIZE_BUTTON)
        versionFont = generateFont(Constants.FONT_SIZE_HUD)

        bgTexture = Texture(Gdx.files.internal("themes/grass/background.png")).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        buttonNormalTexture = Texture(Gdx.files.internal("ui/button.png")).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        buttonPressedTexture = Texture(Gdx.files.internal("ui/button_pressed.png")).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }

        hasProgress = preferences.getBoolean(Constants.KEY_HAS_PROGRESS, false)
        rebuildButtons()
        skipKeyboardFrames = 2
        Gdx.app.log("MainMenuScreen", "shown, buttons=${buttons.size}, hasProgress=$hasProgress")
    }

    private fun generateFont(size: Int): BitmapFont {
        val fontFile = Gdx.files.internal("fonts/boldpixels.ttf")
        val generator = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(fontFile)
        val parameter = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = size
        // Тёмно-коричневая обводка для контраста (кратно 16 — чёткая)
        parameter.borderWidth = size / 16f
        parameter.borderColor = Constants.FONT_COLOR_OUTLINE
        parameter.characters = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.DEFAULT_CHARS +
            "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюя" +
            "0123456789:/ "
        val generatedFont = generator.generateFont(parameter)
        generator.dispose()
        return generatedFont
    }

    private fun rebuildButtons() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)

        // Кнопки — по спецификации MENU.md (§3.1), декларированы в MenuDefinitions
        val specs = MenuDefinitions.mainMenu(hasProgress).filter { spec ->
            spec.action != MenuDefinitions.Action.QUIT_APP ||
                Gdx.app.type == Application.ApplicationType.Desktop
        }
        val buttonList = specs.map { spec -> spec.label to actionFor(spec.action) }

        buttons.clear()
        // Зазор между кнопками уменьшен ещё на 50 % (было 14, исходно 20)
        val spacing = 7f
        // Центрируем группу кнопок по вертикали; если не влезает — прижимаем к краям
        val totalHeight = buttonList.size * Constants.BUTTON_HEIGHT + (buttonList.size - 1) * spacing
        var startY = Constants.WORLD_HEIGHT / 2f + totalHeight / 2f - Constants.BUTTON_HEIGHT
        val topMax = Constants.WORLD_HEIGHT - 10f
        val bottomMin = 10f + (buttonList.size - 1) * (Constants.BUTTON_HEIGHT + spacing)
        startY = startY.coerceIn(bottomMin, topMax)

        for ((index, pair) in buttonList.withIndex()) {
            val (text, action) = pair
            buttons.add(
                UiButton(
                    bounds = Rectangle(
                        Constants.WORLD_WIDTH / 2f - Constants.BUTTON_WIDTH / 2f,
                        startY - index * (Constants.BUTTON_HEIGHT + spacing),
                        Constants.BUTTON_WIDTH,
                        Constants.BUTTON_HEIGHT
                    ),
                    text = text,
                    font = buttonFont,
                    normalTexture = buttonNormalTexture,
                    pressedTexture = buttonPressedTexture
                ) {
                    Gdx.app.log("MainMenuScreen", "button clicked: $text")
                    action()
                }
            )
        }
        focusedIndex = 0
    }

    /** Действие кнопки из MenuDefinitions → конкретный колбэк экрана. */
    private fun actionFor(action: MenuDefinitions.Action): () -> Unit = when (action) {
        MenuDefinitions.Action.START_NEW -> onStartGame
        MenuDefinitions.Action.CONTINUE -> onContinueGame
        MenuDefinitions.Action.QUIT_APP -> onExit
        else -> ({})
    }

    override fun render(delta: Float) {
        if (skipKeyboardFrames > 0) skipKeyboardFrames--
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Выход по BACK на Android
        if (Gdx.app.type == Application.ApplicationType.Android && Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            onExit()
            return
        }

        // ESC — диалог выхода только на Desktop (на Android BACK выше)
        if (Gdx.app.type == Application.ApplicationType.Desktop && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            onExit()
            return
        }

        // Обработка тачей — отслеживаем переход нажато/отпущено,
        // чтобы touchUp срабатывал один раз при отпускании, а не каждый кадр.
        if (Gdx.input.justTouched()) {
            val (touchX, touchY) = unprojectTouch()
            buttons.forEachIndexed { index, button ->
                if (button.touchDown(touchX, touchY)) {
                    pointerDown = true
                    pressedButtonIndex = index
                }
            }
        }
        if (pointerDown && Gdx.input.isTouched(0).not()) {
            val (touchX, touchY) = unprojectTouch()
            buttons.forEach { it.touchUp(touchX, touchY) }
            pointerDown = false
            pressedButtonIndex = -1
        }

        // Навигация клавишами (пропуск первых кадров — см. skipKeyboardFrames)
        if (!pointerDown && skipKeyboardFrames == 0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                focusedIndex = (focusedIndex - 1 + buttons.size) % buttons.size
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                focusedIndex = (focusedIndex + 1) % buttons.size
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            ) {
                buttons.getOrNull(focusedIndex)?.onClick?.invoke()
            }
        }

        // Визуальный фокус: при таче показываем нажатую кнопку,
        // иначе — клавиатурный фокус.
        buttons.forEachIndexed { index, button ->
            button.isPressed = if (pointerDown) index == pressedButtonIndex else index == focusedIndex
        }

        // Фон
        batch.begin()
        bgTexture?.let { bg ->
            val region = com.badlogic.gdx.graphics.g2d.TextureRegion(bg)
            for (x in 0 until Constants.WORLD_WIDTH.toInt() step 40) {
                for (y in 0 until Constants.WORLD_HEIGHT.toInt() step 40) {
                    batch.draw(region, x.toFloat(), y.toFloat(), 40f, 40f)
                }
            }
        }

        // Титул
        val title = MenuDefinitions.TITLE_MENU
        val titleLayout = GlyphLayout(titleFont, title)
        titleFont.color.set(Constants.FONT_COLOR_TEXT)
        titleFont.draw(
            batch,
            title,
            Constants.WORLD_WIDTH / 2f - titleLayout.width / 2f,
            Constants.WORLD_HEIGHT - 40f
        )

        // Рекорд временно не отображается (счёт не ведём)

        // Версия
        val versionText = "v${Constants.VERSION}${LaunchMode.suffix}"
        val versionLayout = GlyphLayout(versionFont, versionText)
        versionFont.color.set(Constants.FONT_COLOR_TEXT)
        versionFont.draw(
            batch,
            versionText,
            Constants.WORLD_WIDTH - versionLayout.width - 20f,
            30f
        )

        // Кнопки
        buttons.forEach { it.draw(batch) }
        batch.end()
    }

    private fun unprojectTouch(): Pair<Float, Float> {
        val touchPos = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        viewport.unproject(touchPos)
        return touchPos.x to touchPos.y
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        titleFont.dispose()
        buttonFont.dispose()
        versionFont.dispose()
        bgTexture?.dispose()
        buttonNormalTexture?.dispose()
        buttonPressedTexture?.dispose()
    }
}
