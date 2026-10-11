package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport
import com.catsandmouses.audio.AudioManager
import com.catsandmouses.i18n.I18n

/**
 * Финальные титры (MENU.md §3.8): фон системной темы тайлами, текст плавно прокручивается
 * снизу вверх (как в «Звёздных войнах»). После прокрутки — пасхалка: карусель фото из
 * `assets/me/` (слегка пикселизованных, во весь экран) и кнопка «Начать сначала»
 * (сброс прогресса, старт с мира 1, карты 1).
 */
class CreditsScreen(
    private val theme: ThemeLoader,
    private val audio: AudioManager,
    private val onStartOver: () -> Unit,
    /** Тап/клавиша во время прокрутки титров (до фото) — в главное меню со сбросом прогресса. */
    private val onSkipToMenu: () -> Unit
) : Screen {

    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private var bgTexture: Texture? = null
    private var buttonNormalTexture: Texture? = null
    private var buttonPressedTexture: Texture? = null

    /** Пикселизованные фото пасхалки (карусель). */
    private val photoTextures = mutableListOf<Texture>()
    private var photoIndex = 0
    private var carouselTimer = 0f

    private lateinit var scroll: CreditsScroll
    private lateinit var startButton: UiButton
    private var lineHeight = 0f
    private val lines = mutableListOf<String>()

    private var pointerDown = false
    // Защита от «протёкшего» ввода при входе с экрана финала
    private var skipFrames = 2

    override fun show() {
        Gdx.app.log("CreditsScreen", "show()")
        // Музыка заставки/меню (Ending) — подходит для титров
        audio.playMusic(AudioManager.TRACK_MENU)
        // Отвязываем обработчик ввода предыдущего экрана (мы опрашиваем ввод в render)
        Gdx.input.inputProcessor = null

        camera = OrthographicCamera(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)
        camera.position.set(Constants.WORLD_WIDTH / 2f, Constants.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        viewport = FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, camera)
        batch = SpriteBatch()
        batch.projectionMatrix = camera.combined
        font = generateFont(FONT_SIZE)

        val nearest = Texture.TextureFilter.Nearest
        bgTexture = Texture(Gdx.files.internal(theme.backgroundPath)).apply { setFilter(nearest, nearest) }
        buttonNormalTexture = Texture(Gdx.files.internal("ui/button.png")).apply { setFilter(nearest, nearest) }
        buttonPressedTexture = Texture(Gdx.files.internal("ui/button_pressed.png")).apply { setFilter(nearest, nearest) }
        loadPhotos()

        lines.clear()
        lines.addAll(CreditsText.lines(I18n.lang))
        lineHeight = font.capHeight + LINE_GAP
        scroll = CreditsScroll(
            totalHeight = lines.size * lineHeight,
            viewportHeight = Constants.WORLD_HEIGHT,
            speed = SCROLL_SPEED
        )

        startButton = UiButton(
            bounds = Rectangle(
                Constants.WORLD_WIDTH / 2f - Constants.BUTTON_WIDTH / 2f,
                BUTTON_Y,
                Constants.BUTTON_WIDTH,
                Constants.BUTTON_HEIGHT
            ),
            text = MenuDefinitions.btnStartOver(),
            font = font,
            normalTexture = buttonNormalTexture,
            pressedTexture = buttonPressedTexture,
            onClick = { startOver() }
        )
        skipFrames = 2
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
            "0123456789:/% "
        val generated = generator.generateFont(parameter)
        generator.dispose()
        return generated
    }

    private fun startOver() {
        Gdx.app.log("CreditsScreen", "start over")
        audio.playSfx("click")
        onStartOver()
    }

    /**
     * Загрузить все фото пасхалки (`assets/me/photos.json`). Файлы уже пикселизованы
     * (готовит dev-скрипт `tools/make_me_photos.py`), рисуем их как есть с фильтром Nearest.
     */
    private fun loadPhotos() {
        photoTextures.clear()
        photoIndex = 0
        val nearest = Texture.TextureFilter.Nearest
        for (path in MePhotos.paths()) {
            val file = Gdx.files.internal(path)
            if (!file.exists()) continue
            photoTextures.add(Texture(file).apply { setFilter(nearest, nearest) })
        }
    }

    /** Текущее фото на весь экран (пропорции сохраняются, по центру). */
    private fun drawCurrentPhoto() {
        val tex = photoTextures.getOrNull(photoIndex) ?: return
        val scale = minOf(
            Constants.WORLD_WIDTH / tex.width.toFloat(),
            Constants.WORLD_HEIGHT / tex.height.toFloat()
        )
        val w = tex.width * scale
        val h = tex.height * scale
        batch.draw(tex, (Constants.WORLD_WIDTH - w) / 2f, (Constants.WORLD_HEIGHT - h) / 2f, w, h)
    }

    private fun nextPhoto() {
        if (photoTextures.isNotEmpty()) photoIndex = (photoIndex + 1) % photoTextures.size
    }

    private fun prevPhoto() {
        if (photoTextures.isNotEmpty()) photoIndex = (photoIndex - 1 + photoTextures.size) % photoTextures.size
    }

    override fun render(delta: Float) {
        if (skipFrames > 0) skipFrames--
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        scroll.update(delta)

        // Фон системной темы — тайлы 1:1 по всему экрану (как заставка/меню)
        batch.begin()
        bgTexture?.let { bg ->
            var y = 0f
            while (y < Constants.WORLD_HEIGHT) {
                var x = 0f
                while (x < Constants.WORLD_WIDTH) {
                    batch.draw(bg, x, y, bg.width.toFloat(), bg.height.toFloat())
                    x += bg.width
                }
                y += bg.height
            }
        }

        // Текст титров — построчно, снизу вверх; за пределами экрана не рисуем
        val blockBottom = scroll.offset
        font.color.set(Constants.FONT_COLOR_TEXT)
        for ((i, line) in lines.withIndex()) {
            if (line.isEmpty()) continue
            val y = blockBottom + (lines.size - 1 - i) * lineHeight
            if (y < -lineHeight || y > Constants.WORLD_HEIGHT + lineHeight) continue
            val layout = GlyphLayout(font, line)
            font.draw(batch, line, Constants.WORLD_WIDTH / 2f - layout.width / 2f, y)
        }
        batch.end()

        // После прокрутки: пасхалка — карусель фото и кнопка «Начать сначала»
        if (scroll.isFinished) {
            carouselTimer += delta
            if (photoTextures.size > 1 && carouselTimer >= CAROUSEL_INTERVAL) {
                carouselTimer = 0f
                nextPhoto()
            }
            handleReadyInput()
            startButton.isFocused = true
            batch.begin()
            drawCurrentPhoto()
            startButton.draw(batch)
            batch.end()
        } else {
            handleSkipInput()
        }
    }

    /**
     * Во время прокрутки титров любой тап/клавиша — пропустить пасхалку и уйти в меню.
     * Карусель фото видят только те, кто титры не трогает (это пасхалка, MENU.md §3.8).
     */
    private fun handleSkipInput() {
        if (skipFrames > 0) return
        val skipped = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.BACK) ||
            Gdx.input.justTouched()
        if (skipped) onSkipToMenu()
    }

    private fun handleReadyInput() {
        if (skipFrames > 0) return
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            startButton.triggerClick()
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            nextPhoto()
            carouselTimer = 0f
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            prevPhoto()
            carouselTimer = 0f
        }
        if (!pointerDown && Gdx.input.justTouched()) {
            val (x, y) = unprojectTouch()
            if (startButton.touchDown(x, y)) {
                pointerDown = true
            } else {
                // Тап мимо кнопки — следующее фото
                nextPhoto()
                carouselTimer = 0f
            }
        }
        if (pointerDown && !Gdx.input.isTouched(0)) {
            val (x, y) = unprojectTouch()
            startButton.touchUp(x, y)
            pointerDown = false
        }
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
        font.dispose()
        bgTexture?.dispose()
        photoTextures.forEach { it.dispose() }
        photoTextures.clear()
        buttonNormalTexture?.dispose()
        buttonPressedTexture?.dispose()
    }

    companion object {
        // Размер кратен 16 — пиксель-перфект BoldPixels (как остальные шрифты UI)
        private const val FONT_SIZE = 32
        private const val LINE_GAP = 12f
        private const val SCROLL_SPEED = 30f
        // Пасхалка-карусель: интервал автопрокрутки, сек
        private const val CAROUSEL_INTERVAL = 6f
        // Кнопка «Начать сначала» — внизу экрана
        private const val BUTTON_Y = 60f
    }
}
