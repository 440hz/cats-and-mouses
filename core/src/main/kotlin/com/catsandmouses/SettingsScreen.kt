package com.catsandmouses

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
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
import com.catsandmouses.audio.AudioManager
import com.catsandmouses.i18n.I18n

/**
 * Экран «Настройки» (MENU.md §3.7): два ползунка («Музыка», «Звуки», шаг 10),
 * переключатель языка «Язык / Language» (RU/EN) и кнопка «Назад».
 * Фон — системная тема тайлами, как в меню.
 */
class SettingsScreen(
    private val theme: ThemeLoader,
    private val progress: ProgressStore,
    private val audio: AudioManager,
    private val onBack: () -> Unit
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
    private var trackTexture: Texture? = null
    private var knobTexture: Texture? = null

    private lateinit var musicSlider: UiSlider
    private lateinit var sfxSlider: UiSlider
    private lateinit var backButton: UiButton

    /** Переключатель языка (дорожка-фон + `< Значение >`); тап/клик или ←/→ меняют RU ⇄ EN. */
    private val languageBox = Rectangle(
        Constants.WORLD_WIDTH / 2f - Constants.SLIDER_WIDTH / 2f,
        200f,
        Constants.SLIDER_WIDTH,
        90f
    )

    /** Фокус: 0 — «Музыка», 1 — «Звуки», 2 — «Язык», 3 — «Назад». */
    private var focusedIndex = 0
    private var pointerDown = false
    private var pressedBack = false
    private var draggingSlider = false
    // Защита от «протёкшего» нажатия ESC/клавиши при входе с предыдущего экрана.
    private var skipKeyboardFrames = 2

    override fun show() {
        audio.playMusic(AudioManager.TRACK_MENU)

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

        val nearest = Texture.TextureFilter.Nearest
        bgTexture = Texture(Gdx.files.internal(theme.backgroundPath)).apply { setFilter(nearest, nearest) }
        buttonNormalTexture = Texture(Gdx.files.internal("ui/button.png")).apply { setFilter(nearest, nearest) }
        buttonPressedTexture = Texture(Gdx.files.internal("ui/button_pressed.png")).apply { setFilter(nearest, nearest) }
        trackTexture = Texture(Gdx.files.internal("ui/slider_track.png")).apply { setFilter(nearest, nearest) }
        knobTexture = Texture(Gdx.files.internal("ui/slider_knob.png")).apply { setFilter(nearest, nearest) }

        Gdx.input.setCatchKey(Input.Keys.BACK, true)

        val sliderX = Constants.WORLD_WIDTH / 2f - Constants.SLIDER_WIDTH / 2f
        musicSlider = UiSlider(
            bounds = Rectangle(sliderX, 480f, Constants.SLIDER_WIDTH, Constants.SLIDER_HEIGHT),
            value = progress.musicVolume(),
            label = musicLabel(progress.musicVolume()),
            font = buttonFont,
            trackTexture = trackTexture,
            knobTexture = knobTexture,
            labelTop = 615f
        )
        sfxSlider = UiSlider(
            bounds = Rectangle(sliderX, 326f, Constants.SLIDER_WIDTH, Constants.SLIDER_HEIGHT),
            value = progress.sfxVolume(),
            label = sfxLabel(progress.sfxVolume()),
            font = buttonFont,
            trackTexture = trackTexture,
            knobTexture = knobTexture,
            labelTop = 471f
        )

        backButton = UiButton(
            bounds = Rectangle(
                Constants.WORLD_WIDTH / 2f - Constants.BUTTON_WIDTH / 2f,
                30f,
                Constants.BUTTON_WIDTH,
                140f
            ),
            text = MenuDefinitions.btnBack(),
            font = buttonFont,
            normalTexture = buttonNormalTexture,
            pressedTexture = buttonPressedTexture,
            onClick = { goBack() }
        )

        skipKeyboardFrames = 2
        Gdx.app.log(
            "SettingsScreen",
            "shown, lang=${I18n.lang.code} music=${musicSlider.value} sfx=${sfxSlider.value}"
        )
    }

    private fun generateFont(size: Int): BitmapFont {
        val fontFile = Gdx.files.internal("fonts/boldpixels.ttf")
        val generator = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(fontFile)
        val parameter = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = size
        parameter.borderWidth = size / 16f
        parameter.borderColor = Constants.FONT_COLOR_OUTLINE
        parameter.characters = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.DEFAULT_CHARS +
            "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюя" +
            "0123456789:/% "
        val generatedFont = generator.generateFont(parameter)
        generator.dispose()
        return generatedFont
    }

    private fun musicLabel(value: Int): String = MenuDefinitions.volumeLabel(MenuDefinitions.labelMusic(), value)

    private fun sfxLabel(value: Int): String = MenuDefinitions.volumeLabel(MenuDefinitions.labelSfx(), value)

    /** Переключатель языка: дорожка-фон + «Язык: < Рус >»; при фокусе только акцент, без сдвига. */
    private fun drawLanguage(batch: SpriteBatch) {
        val focused = focusedIndex == 2
        trackTexture?.let { batch.draw(it, languageBox.x, languageBox.y, languageBox.width, languageBox.height) }
        val text = "${MenuDefinitions.labelLanguage()}: < ${MenuDefinitions.languageValue()} >"
        val layout = GlyphLayout(buttonFont, text)
        buttonFont.color.set(if (focused) Constants.FONT_COLOR_ACCENT else Constants.FONT_COLOR_TEXT)
        buttonFont.draw(
            batch,
            text,
            languageBox.x + (languageBox.width - layout.width) / 2f,
            languageBox.y + (languageBox.height + layout.height) / 2f
        )
        buttonFont.color.set(Constants.FONT_COLOR_TEXT)
    }

    private fun goBack() {
        Gdx.app.log("SettingsScreen", "back to menu")
        onBack()
    }

    private fun onMusicChanged() {
        val v = musicSlider.value
        musicSlider.label = musicLabel(v)
        progress.setMusicVolume(v)
        audio.setMusicVolume(v)
    }

    private fun onSfxChanged() {
        val v = sfxSlider.value
        val previous = progress.sfxVolume()
        sfxSlider.label = sfxLabel(v)
        progress.setSfxVolume(v)
        audio.setSfxVolume(v)
        // Щелчок-подтверждение только при реальном изменении значения
        if (v != previous) audio.playSfx("click")
    }

    /** Переключить язык интерфейса: применить ко всем подписям и сохранить. */
    private fun onLanguageChanged() {
        I18n.toggle()
        progress.setLanguage(I18n.lang.code)
        // Подписи ползунков и кнопки берутся из словаря — обновляем на месте
        musicSlider.label = musicLabel(musicSlider.value)
        sfxSlider.label = sfxLabel(sfxSlider.value)
        backButton.text = MenuDefinitions.btnBack()
        audio.playSfx("click")
        Gdx.app.log("SettingsScreen", "language switched to ${I18n.lang.code}")
    }

    private fun handleInput() {
        if (Gdx.app.type == Application.ApplicationType.Android &&
            Gdx.input.isKeyJustPressed(Input.Keys.BACK)
        ) {
            goBack()
            return
        }
        if (Gdx.app.type == Application.ApplicationType.Desktop &&
            Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
        ) {
            goBack()
            return
        }

        // Тач/клик: ползунки (drag), переключатель языка и кнопка «Назад»
        if (!pointerDown && Gdx.input.justTouched()) {
            val (x, y) = unprojectTouch()
            var handled = false
            if (musicSlider.touchDown(x, y)) {
                focusedIndex = 0
                draggingSlider = true
                handled = true
                onMusicChanged()
            } else if (sfxSlider.touchDown(x, y)) {
                focusedIndex = 1
                draggingSlider = true
                handled = true
                onSfxChanged()
            } else if (languageBox.contains(x, y)) {
                focusedIndex = 2
                handled = true
                onLanguageChanged()
            }
            if (!handled && backButton.touchDown(x, y)) {
                pressedBack = true
            }
            pointerDown = true
        }
        if (pointerDown && draggingSlider && Gdx.input.isTouched(0)) {
            val (x, _) = unprojectTouch()
            if (musicSlider.isDragging) {
                musicSlider.drag(x)
                onMusicChanged()
            } else if (sfxSlider.isDragging) {
                sfxSlider.drag(x)
                onSfxChanged()
            }
        }
        if (pointerDown && !Gdx.input.isTouched(0)) {
            val (x, y) = unprojectTouch()
            if (draggingSlider) {
                musicSlider.touchUp()
                sfxSlider.touchUp()
                draggingSlider = false
            }
            if (pressedBack) {
                backButton.touchUp(x, y)
                pressedBack = false
            }
            pointerDown = false
            return
        }

        // Клавиатура: ↑/↓ — фокус, ←/→ — шаг/переключение, Space/Enter — «Назад»
        if (!pointerDown && skipKeyboardFrames == 0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                focusedIndex = (focusedIndex - 1 + 4) % 4
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                focusedIndex = (focusedIndex + 1) % 4
            }
            val step = Constants.VOLUME_STEP
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                when (focusedIndex) {
                    0 -> { musicSlider.step(-step); onMusicChanged() }
                    1 -> { sfxSlider.step(-step); onSfxChanged() }
                    2 -> onLanguageChanged()
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                when (focusedIndex) {
                    0 -> { musicSlider.step(step); onMusicChanged() }
                    1 -> { sfxSlider.step(step); onSfxChanged() }
                    2 -> onLanguageChanged()
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            ) {
                if (focusedIndex == 3) goBack()
            }
        }
    }

    override fun render(delta: Float) {
        if (skipKeyboardFrames > 0) skipKeyboardFrames--
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        handleInput()

        val keyDown = Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isKeyPressed(Input.Keys.ENTER)
        musicSlider.isFocused = focusedIndex == 0
        sfxSlider.isFocused = focusedIndex == 1
        backButton.isFocused = focusedIndex == 3
        backButton.isPressed = pressedBack || (focusedIndex == 3 && keyDown && !pointerDown)

        // 1) Фон, заголовок, дорожки, подписи и строка языка
        batch.begin()
        bgTexture?.let { bg ->
            val region = com.badlogic.gdx.graphics.g2d.TextureRegion(bg)
            for (x in 0 until Constants.WORLD_WIDTH.toInt() + bg.width step bg.width) {
                for (y in 0 until Constants.WORLD_HEIGHT.toInt() + bg.height step bg.height) {
                    batch.draw(region, x.toFloat(), y.toFloat(), bg.width.toFloat(), bg.height.toFloat())
                }
            }
        }
        val title = MenuDefinitions.titleSettings()
        val titleLayout = GlyphLayout(titleFont, title)
        titleFont.color.set(Constants.FONT_COLOR_TEXT)
        titleFont.draw(batch, title, Constants.WORLD_WIDTH / 2f - titleLayout.width / 2f, Constants.WORLD_HEIGHT - 40f)

        musicSlider.drawTrackAndLabel(batch)
        sfxSlider.drawTrackAndLabel(batch)

        // Переключатель языка: дорожка-фон + надпись «Язык: < Рус >»
        drawLanguage(batch)
        batch.end()

        // 2) Насечки поверх заливки, под ручкой
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        musicSlider.drawNotches(shapeRenderer)
        sfxSlider.drawNotches(shapeRenderer)
        shapeRenderer.end()

        // 3) Ручки, кнопка и версия
        batch.begin()
        musicSlider.drawKnob(batch)
        sfxSlider.drawKnob(batch)
        backButton.draw(batch)

        val versionText = "v${Constants.VERSION}${LaunchMode.suffix}"
        val versionLayout = GlyphLayout(versionFont, versionText)
        versionFont.color.set(Constants.FONT_COLOR_TEXT)
        versionFont.draw(batch, versionText, Constants.WORLD_WIDTH - versionLayout.width - 20f, 30f)
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
        trackTexture?.dispose()
        knobTexture?.dispose()
    }
}
