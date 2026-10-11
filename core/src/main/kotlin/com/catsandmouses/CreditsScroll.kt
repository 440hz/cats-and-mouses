package com.catsandmouses

/**
 * Прокрутка финальных титров снизу вверх (чистая логика, без GL — тестируется headless).
 *
 * [offset] — мировая Y **нижней** границы текстового блока. На старте блок целиком под
 * экраном (`offset = -totalHeight`), затем поднимается со скоростью [speed] px/с.
 * [isFinished] — когда нижняя граница ушла выше верхнего края экрана (`offset >= viewportHeight`).
 */
class CreditsScroll(
    val totalHeight: Float,
    private val viewportHeight: Float,
    private val speed: Float
) {

    var offset: Float = -totalHeight
        private set

    val isFinished: Boolean get() = offset >= viewportHeight

    fun update(delta: Float) {
        if (isFinished) return
        offset = (offset + speed * delta).coerceAtMost(viewportHeight)
    }

    /** Пропустить прокрутку к концу (показать кнопку). */
    fun skip() {
        offset = viewportHeight
    }
}
