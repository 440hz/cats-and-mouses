package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter

/**
 * Обработчик ввода: клавиатура (Desktop) и свайпы (Android).
 * Приоритет у последнего события ввода — «кто последний, тот и прав».
 */
class InputHandler : InputAdapter() {

    companion object {
        // Порог свайпа в экранных пикселях
        private const val SWIPE_THRESHOLD = 40f
    }

    private var keyboardDirection = Direction.NONE
    private var lastKeyboardDirection = Direction.NONE
    private var swipeDirection = Direction.NONE
    private var lastInputDirection = Direction.NONE

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isTouching = false
    private var pauseRequest = false

    /**
     * Возвращает актуальное направление: клавиатура, если клавиша только что изменилась,
     * иначе последнее направление от свайпа. Зажатая клавиша не перебивает свайп каждый кадр.
     */
    fun getDirection(): Direction {
        val up = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)
        val down = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)
        val left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)
        val right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)
        keyboardDirection = Direction.fromKeys(up, down, left, right)

        // Новое нажатие клавиатуры перекрывает свайп
        if (keyboardDirection != Direction.NONE && keyboardDirection != lastKeyboardDirection) {
            lastInputDirection = keyboardDirection
        }
        lastKeyboardDirection = keyboardDirection

        return lastInputDirection
    }

    fun isPauseJustPressed(): Boolean {
        return Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || consumePauseRequest()
    }

    /**
     * Установить направление вручную (например, из AI-режима).
     * Эквивалент свайпа: направление запоминается и применяется при первой возможности.
     */
    fun setDirection(direction: Direction) {
        if (direction != Direction.NONE) {
            lastInputDirection = direction
        }
    }

    /**
     * Вернуть последнее направление ввода (для отладки / AI).
     */
    fun getLastInputDirection(): Direction = lastInputDirection

    /**
     * Переключить паузу (например, из AI-режима).
     */
    fun togglePause() {
        pauseRequest = true
    }

    private fun consumePauseRequest(): Boolean {
        val value = pauseRequest
        pauseRequest = false
        return value
    }

    /**
     * Сброс состояния ввода. Вызывается при старте новой игры,
     * чтобы случайно не сохранилось направление с прошлого раунда.
     */
    fun reset() {
        keyboardDirection = Direction.NONE
        lastKeyboardDirection = Direction.NONE
        swipeDirection = Direction.NONE
        lastInputDirection = Direction.NONE
        isTouching = false
        pauseRequest = false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        touchStartX = screenX.toFloat()
        touchStartY = screenY.toFloat()
        isTouching = true
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!isTouching) return false

        val dx = screenX - touchStartX
        val dy = screenY - touchStartY
        val absDx = kotlin.math.abs(dx)
        val absDy = kotlin.math.abs(dy)

        // Определяем направление свайпа по экранным координатам:
        // y=0 — верх экрана, поэтому dy < 0 — вверх, dy > 0 — вниз
        when {
            absDx > absDy && absDx > SWIPE_THRESHOLD -> {
                swipeDirection = if (dx > 0) Direction.RIGHT else Direction.LEFT
                lastInputDirection = swipeDirection
            }
            absDy > absDx && absDy > SWIPE_THRESHOLD -> {
                swipeDirection = if (dy > 0) Direction.DOWN else Direction.UP
                lastInputDirection = swipeDirection
            }
        }

        isTouching = false
        return true
    }
}
