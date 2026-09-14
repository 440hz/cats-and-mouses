package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3

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
    var textColor: Color = Color.WHITE,
    var onClick: () -> Unit = {}
) {
    var isPressed = false

    fun hit(x: Float, y: Float): Boolean = bounds.contains(x, y)

    fun draw(batch: SpriteBatch) {
        val tex = if (isPressed) pressedTexture ?: normalTexture else normalTexture
        if (tex != null) {
            batch.draw(tex, bounds.x, bounds.y, bounds.width, bounds.height)
        }
        // Fallback (цветной прямоугольник) рисуется вызывающей стороной через ShapeRenderer

        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, text)
        font.color = textColor
        font.draw(
            batch,
            text,
            bounds.x + (bounds.width - layout.width) / 2f,
            bounds.y + (bounds.height + layout.height) / 2f
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
 * Диалог: затемнение + панель с заголовком и кнопками.
 */
class UiDialog(
    val title: String,
    val buttons: List<UiButton>,
    val panelBounds: Rectangle,
    var titleFont: BitmapFont,
    var panelColor: Color = Color(0f, 0f, 0f, 0.85f),
    var borderColor: Color = Color.WHITE
) {
    fun draw(batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        // Затемнение всего экрана
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.6f)
        shapeRenderer.rect(0f, 0f, Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT)
        shapeRenderer.end()

        // Панель
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = panelColor
        shapeRenderer.rect(panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = borderColor
        shapeRenderer.rect(panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height)
        shapeRenderer.end()

        // Заголовок
        batch.begin()
        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(titleFont, title)
        titleFont.color = Color.WHITE
        titleFont.draw(
            batch,
            title,
            panelBounds.x + (panelBounds.width - layout.width) / 2f,
            panelBounds.y + panelBounds.height - 40f
        )
        buttons.forEach { it.draw(batch) }
        batch.end()
    }

    fun touchDown(x: Float, y: Float): Boolean {
        for (button in buttons) {
            if (button.touchDown(x, y)) return true
        }
        return false
    }

    fun touchUp(x: Float, y: Float): Boolean {
        for (button in buttons) {
            if (button.touchUp(x, y)) return true
        }
        return false
    }
}
