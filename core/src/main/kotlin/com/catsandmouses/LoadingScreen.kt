package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport

/**
 * Экран загрузки: логотип, подсказка «нажми любую клавишу»,
 * переход в меню по любому нажатию клавиши или тапу.
 */
class LoadingScreen(
    private val onLoaded: () -> Unit
) : Screen {

    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var titleFont: BitmapFont
    private var logoTexture: Texture? = null
    private var bgTexture: Texture? = null
    private var loaded = false
    private var elapsed = 0f
    private val minStaySeconds = 1f

    override fun show() {
        Gdx.app.log("LoadingScreen", "show()")
        camera = OrthographicCamera(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)
        camera.position.set(Constants.WORLD_WIDTH / 2f, Constants.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        viewport = FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, camera)
        batch = SpriteBatch()
        batch.projectionMatrix = camera.combined
        font = generateFont(Constants.FONT_SIZE_OVERLAY)
        // Заголовок «Кошки-мышки» — светлая (кремовая) заливка + тёмная обводка,
        // как в остальном UI (задаются при генерации, при отрисовке шрифт не тонируется)
        titleFont = generateFont(
            Constants.FONT_SIZE_TITLE,
            borderColor = Constants.FONT_COLOR_OUTLINE,
            textColor = Constants.FONT_COLOR_TEXT
        )

        logoTexture = Texture(Gdx.files.internal("ui/loading_logo_4.png")).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        bgTexture = Texture(Gdx.files.internal("themes/grass/background.png")).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }

        // Переход дальше — только по реальному нажатию клавиши или тапу.
        // Минимальное время показа guard'ит от служебных событий входа (фокус окна).
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                finishAfterMinStay()
                return true
            }

            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                finishAfterMinStay()
                return true
            }
        }
    }

    private fun finishAfterMinStay() {
        if (elapsed >= minStaySeconds) {
            finish()
        }
    }

    private fun finish() {
        if (loaded) return
        loaded = true
        Gdx.app.log("LoadingScreen", "skipped by key/touch, switching to menu")
        onLoaded()
    }

    private fun generateFont(
        size: Int,
        borderColor: com.badlogic.gdx.graphics.Color = Constants.FONT_COLOR_OUTLINE,
        textColor: com.badlogic.gdx.graphics.Color = com.badlogic.gdx.graphics.Color.WHITE
    ): BitmapFont {
        val fontFile = Gdx.files.internal("fonts/boldpixels.ttf")
        val generator = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(fontFile)
        val parameter = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = size
        // Обводка для контраста (кратно 16 — чёткая)
        parameter.borderWidth = size / 16f
        parameter.borderColor = borderColor
        parameter.color = textColor
        parameter.characters = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.DEFAULT_CHARS +
            "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюя" +
            "0123456789:% "
        val generatedFont = generator.generateFont(parameter)
        generator.dispose()
        return generatedFont
    }

    override fun render(delta: Float) {
        elapsed += delta
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Фон темы (трава) по всему экрану тайлами
        batch.begin()
        bgTexture?.let { bg ->
            val bgRegion = com.badlogic.gdx.graphics.g2d.TextureRegion(bg)
            for (x in 0 until Constants.WORLD_WIDTH.toInt() step 40) {
                for (y in 0 until Constants.WORLD_HEIGHT.toInt() step 40) {
                    batch.draw(bgRegion, x.toFloat(), y.toFloat(), 40f, 40f)
                }
            }
        }

        // Логотип по центру
        logoTexture?.let {
            val logoW = 600f
            val logoH = 430f
            batch.draw(it, (Constants.WORLD_WIDTH - logoW) / 2f, 160f, logoW, logoH)
        }

        // Заголовок «Кошки-мышки» (тёмная заливка + светлая обводка; тинт не применяем)
        val titleLayout = GlyphLayout(titleFont, MenuDefinitions.TITLE_GAME)
        titleFont.draw(
            batch,
            MenuDefinitions.TITLE_GAME,
            Constants.WORLD_WIDTH / 2f - titleLayout.width / 2f,
            Constants.WORLD_HEIGHT - 40f
        )

        // Подсказка: любая клавиша / тап — дальше
        val hintText = "Нажми любую клавишу"
        val layout = GlyphLayout(font, hintText)
        font.color.set(Constants.FONT_COLOR_TEXT)
        font.draw(
            batch,
            hintText,
            Constants.WORLD_WIDTH / 2f - layout.width / 2f,
            110f
        )
        batch.end()
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
        titleFont.dispose()
        logoTexture?.dispose()
        bgTexture?.dispose()
    }
}