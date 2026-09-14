package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport

/**
 * Экран загрузки: логотип, полоска прогресса с процентами, переход в меню.
 */
class LoadingScreen(
    private val onLoaded: () -> Unit
) : Screen {

    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var batch: SpriteBatch
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var font: BitmapFont
    private var logoTexture: Texture? = null
    private var bgTexture: Texture? = null
    private var progress = 0f
    private var elapsed = 0f
    private var loaded = false
    private val minLoadTime = 1.2f

    override fun show() {
        Gdx.app.log("LoadingScreen", "show()")
        camera = OrthographicCamera(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)
        camera.position.set(Constants.WORLD_WIDTH / 2f, Constants.WORLD_HEIGHT / 2f, 0f)
        camera.update()
        viewport = FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, camera)
        batch = SpriteBatch()
        batch.projectionMatrix = camera.combined
        shapeRenderer = ShapeRenderer()
        shapeRenderer.projectionMatrix = camera.combined
        font = generateFont()

        logoTexture = Texture(Gdx.files.internal("ui/loading_logo.png")).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        bgTexture = Texture(Gdx.files.internal("themes/grass/background.png")).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
    }

    private fun generateFont(): BitmapFont {
        val fontFile = Gdx.files.internal("fonts/ptsans.ttf")
        val generator = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(fontFile)
        val parameter = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = Constants.FONT_SIZE_OVERLAY
        parameter.characters = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.DEFAULT_CHARS +
            "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
            "абвгдеёжзийклмнопрстуфхцчшщъыьэюя" +
            "0123456789:% "
        val generatedFont = generator.generateFont(parameter)
        generator.dispose()
        return generatedFont
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        elapsed += delta
        progress = (elapsed / minLoadTime).coerceIn(0f, 1f)

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
            val logoW = 400f
            val logoH = 200f
            batch.draw(it, Constants.WORLD_WIDTH / 2f - logoW / 2f, Constants.WORLD_HEIGHT / 2f + 50f, logoW, logoH)
        }

        // Текст "Загрузка..."
        val loadingText = "Загрузка... ${(progress * 100).toInt()}%"
        val layout = GlyphLayout(font, loadingText)
        font.color = Color.WHITE
        font.draw(
            batch,
            loadingText,
            Constants.WORLD_WIDTH / 2f - layout.width / 2f,
            Constants.WORLD_HEIGHT / 2f - 80f
        )
        batch.end()

        // Полоска прогресса
        val barW = 400f
        val barH = 24f
        val barX = Constants.WORLD_WIDTH / 2f - barW / 2f
        val barY = Constants.WORLD_HEIGHT / 2f - 130f
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.DARK_GRAY
        shapeRenderer.rect(barX, barY, barW, barH)
        shapeRenderer.color = Color(0.2f, 0.8f, 0.2f, 1f)
        shapeRenderer.rect(barX, barY, barW * progress, barH)
        shapeRenderer.end()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = Color.WHITE
        shapeRenderer.rect(barX, barY, barW, barH)
        shapeRenderer.end()

        if (progress >= 1f && !loaded) {
            loaded = true
            Gdx.app.log("LoadingScreen", "done, switching to menu")
            onLoaded()
        }
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
        font.dispose()
        logoTexture?.dispose()
        bgTexture?.dispose()
    }
}
