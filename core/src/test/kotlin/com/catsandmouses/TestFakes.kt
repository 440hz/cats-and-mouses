package com.catsandmouses

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Вспомогательные объекты для тестов, которым нужен «пустой» LibGDX-объект
 * без OpenGL (шрифт создаётся из пустых данных без текстуры).
 */
object TestFakes {

    /** Пустой [BitmapFont] без GL: пригоден для конструкторов UI, но не для отрисовки. */
    fun font(): BitmapFont = BitmapFont(BitmapFont.BitmapFontData(), TextureRegion(), false)
}
