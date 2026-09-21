package com.catsandmouses.graphics

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Покадровая анимация из горизонтального спрайт-листа.
 * Общий механизм для всех анимированных спрайтов (сыр, дальше — мышь, коты, сердца).
 *
 * Кадры листа одинаковой ширины; время анимации ведёт вызывающая сторона
 * (рендер), а фаза позволяет разнести одинаковые спрайты по циклу.
 */
class AnimatedSprite(
    private val texture: Texture,
    val frameCount: Int,
    val frameDuration: Float
) {

    private val animation: Animation<TextureRegion>

    init {
        require(frameCount > 0) { "frameCount должен быть > 0" }
        require(frameDuration > 0f) { "frameDuration должен быть > 0" }
        val frameWidth = texture.width / frameCount
        require(frameWidth > 0) { "Текстура уже, чем число кадров" }
        val frames = TextureRegion.split(texture, frameWidth, texture.height)[0]
        animation = Animation(frameDuration, *frames).apply { playMode = Animation.PlayMode.LOOP }
    }

    /** Кадр для текущего времени (с необязательным сдвигом фазы в секундах). */
    fun regionAt(elapsed: Float, phase: Float = 0f): TextureRegion =
        animation.getKeyFrame(elapsed + phase, true)

    fun dispose() = texture.dispose()

    companion object {
        /** Индекс кадра по времени и фазе. Чистая логика — тестируется без GL. */
        fun frameIndexAt(elapsed: Float, phase: Float, frameCount: Int, frameDuration: Float): Int {
            require(frameCount > 0) { "frameCount должен быть > 0" }
            require(frameDuration > 0f) { "frameDuration должен быть > 0" }
            val cycle = frameCount * frameDuration
            var t = (elapsed + phase) % cycle
            if (t < 0f) t += cycle
            return (t / frameDuration).toInt().coerceIn(0, frameCount - 1)
        }
    }
}
