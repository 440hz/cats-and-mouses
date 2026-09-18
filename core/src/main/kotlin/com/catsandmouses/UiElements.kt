package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3

/**
 * Общий интерфейс оверлея: диалог или полноэкранное меню.
 */
interface UiOverlay {
    fun draw(batch: SpriteBatch, shapeRenderer: ShapeRenderer)
    fun touchDown(x: Float, y: Float): Boolean
    fun touchUp(x: Float, y: Float): Boolean
}

/**
 * Кнопка UI: фон-текстура (normal/pressed) + текст.
 * Если текстура не задана — рисуется цветной прямоугольник.
 */
class UiButton(
    val bounds: Rectangle,
    var text: String,
    var font: BitmapFont,
    var normalTexture: Texture? = null,
    var pressedTexture: Texture? = null,
    var bgColor: Color = Color.DARK_GRAY,
    var pressedColor: Color = Color.GRAY,
    var textColor: Color = Constants.FONT_COLOR_TEXT,
    var pressedTextColor: Color = Constants.FONT_COLOR_ACCENT,
    var onClick: () -> Unit = {}
) {
    var isFocused = false
    var isPressed = false

    fun hit(x: Float, y: Float): Boolean = bounds.contains(x, y)

    fun draw(batch: SpriteBatch) {
        // Активная (под фокусом) кнопка приподнята влево-вверх; при нажатии возвращается
        // на место и меняет текстуру на обычную — эффект отжима.
        val active = isFocused && !isPressed
        val lift = if (active) LIFT else 0f
        val drawX = bounds.x - lift
        val drawY = bounds.y + lift
        val tex = if (active) pressedTexture ?: normalTexture else normalTexture
        if (tex != null) {
            batch.draw(tex, drawX, drawY, bounds.width, bounds.height)
        }
        // Fallback (цветной прямоугольник) рисуется вызывающей стороной через ShapeRenderer

        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, text)
        font.color = if (active) pressedTextColor else textColor
        font.draw(
            batch,
            text,
            drawX + (bounds.width - layout.width) / 2f,
            drawY + (bounds.height + layout.height) / 2f
        )
    }

    fun touchDown(x: Float, y: Float): Boolean {
        if (hit(x, y)) {
            isPressed = true
            return true
        }
        return false
    }

    fun touchUp(x: Float, y: Float): Boolean {
        val wasPressed = isPressed
        isPressed = false
        if (wasPressed && hit(x, y)) {
            onClick()
            return true
        }
        return false
    }

    /**
     * Программное нажатие кнопки: визуально нажать, выполнить действие, отпустить.
     * Используется для Space/Enter и т.п.
     */
    fun triggerClick() {
        isPressed = true
        onClick()
        isPressed = false
    }

    companion object {
        /** Сдвиг активной кнопки: влево и вверх (мировые пиксели). */
        const val LIFT = 10f
    }
}

/**
 * Панель: полупрозрачный фон с рамкой.
 */
class UiPanel(
    val bounds: Rectangle,
    var color: Color = Color(0f, 0f, 0f, 0.75f),
    var borderColor: Color = Color.WHITE,
    var borderWidth: Float = 2f
) {
    fun draw(shapeRenderer: ShapeRenderer) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = color
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = borderColor
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height)
        shapeRenderer.end()
    }
}

/**
 * Полноэкранный оверлей в стиле главного меню: фон темы тайлами, заголовок сверху
 * (тот же отступ, что у заставки/меню) и кнопки столбцом. Без панели и затемнения.
 */
class UiMenuOverlay(
    var title: String,
    val buttons: List<UiButton>,
    var titleFont: BitmapFont,
    var backgroundRegion: TextureRegion? = null,
    var backgroundCell: Float = 40f,
    var titleTopOffset: Float = 40f
) : UiOverlay {

    // Активная (по умолчанию — верхняя) кнопка
    var focusedIndex = 0
    private var pointerPressedIndex = -1

    /** Сместить фокус по списку кнопок с зацикливанием. */
    fun moveFocus(delta: Int) {
        if (buttons.isEmpty()) return
        focusedIndex = (focusedIndex + delta + buttons.size) % buttons.size
    }

    /** Активировать кнопку под фокусом (Space/Enter). */
    fun triggerFocused() {
        buttons.getOrNull(focusedIndex)?.triggerClick()
    }

    override fun draw(batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        // Фокус — активная (приподнятая) кнопка; удержание Space/Enter или тач/клик —
        // «отжим» на место. См. UiButton.draw.
        val keyDown = Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isKeyPressed(Input.Keys.ENTER)
        buttons.forEachIndexed { index, button ->
            button.isFocused = index == focusedIndex
            button.isPressed = (pointerPressedIndex >= 0 && index == pointerPressedIndex) ||
                (index == focusedIndex && keyDown && pointerPressedIndex < 0)
        }

        batch.begin()
        // Фон темы — тайлы по всему экрану
        backgroundRegion?.let { region ->
            var y = 0f
            while (y < Constants.WORLD_HEIGHT) {
                var x = 0f
                while (x < Constants.WORLD_WIDTH) {
                    batch.draw(region, x, y, backgroundCell, backgroundCell)
                    x += backgroundCell
                }
                y += backgroundCell
            }
        }

        // Заголовок
        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(titleFont, title)
        titleFont.color.set(Constants.FONT_COLOR_TEXT)
        titleFont.draw(
            batch,
            title,
            Constants.WORLD_WIDTH / 2f - layout.width / 2f,
            Constants.WORLD_HEIGHT - titleTopOffset
        )

        // Кнопки
        buttons.forEach { it.draw(batch) }
        batch.end()
    }

    override fun touchDown(x: Float, y: Float): Boolean {
        buttons.forEachIndexed { index, button ->
            if (button.touchDown(x, y)) {
                pointerPressedIndex = index
                focusedIndex = index
                return true
            }
        }
        return false
    }

    override fun touchUp(x: Float, y: Float): Boolean {
        var clicked = false
        buttons.forEach { if (it.touchUp(x, y)) clicked = true }
        pointerPressedIndex = -1
        return clicked
    }
}
