package com.catsandmouses

import com.badlogic.gdx.Input
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InputHandlerTest : GdxTestBase() {

    private val handler = InputHandler()

    @Test
    fun `без ввода направление NONE`() {
        assertEquals(Direction.NONE, handler.getDirection())
    }

    @Test
    fun `распознаёт стрелку вверх`() {
        fakeInput.pressKey(Input.Keys.UP)
        assertEquals(Direction.UP, handler.getDirection())
    }

    @Test
    fun `распознаёт WASD`() {
        fakeInput.pressKey(Input.Keys.W)
        assertEquals(Direction.UP, handler.getDirection())

        fakeInput.releaseKey(Input.Keys.W)
        fakeInput.pressKey(Input.Keys.A)
        assertEquals(Direction.LEFT, handler.getDirection())

        fakeInput.releaseKey(Input.Keys.A)
        fakeInput.pressKey(Input.Keys.S)
        assertEquals(Direction.DOWN, handler.getDirection())

        fakeInput.releaseKey(Input.Keys.S)
        fakeInput.pressKey(Input.Keys.D)
        assertEquals(Direction.RIGHT, handler.getDirection())
    }

    @Test
    fun `свайп вправо задаёт направление`() {
        handler.touchDown(0, 0, 0, 0)
        handler.touchUp(100, 0, 0, 0)
        assertEquals(Direction.RIGHT, handler.getDirection())
    }

    @Test
    fun `свайп влево вверх и вниз`() {
        handler.touchDown(100, 100, 0, 0)
        handler.touchUp(0, 100, 0, 0)
        assertEquals(Direction.LEFT, handler.getDirection())

        handler.touchDown(100, 100, 0, 0)
        handler.touchUp(100, 200, 0, 0)
        assertEquals(Direction.DOWN, handler.getDirection())

        handler.touchDown(100, 100, 0, 0)
        handler.touchUp(100, 0, 0, 0)
        assertEquals(Direction.UP, handler.getDirection())
    }

    @Test
    fun `свайп короче порога игнорируется`() {
        handler.touchDown(0, 0, 0, 0)
        handler.touchUp(10, 0, 0, 0)
        assertEquals(Direction.NONE, handler.getDirection())
    }

    @Test
    fun `диагональный свайп определяется по доминирующей оси`() {
        handler.touchDown(0, 0, 0, 0)
        handler.touchUp(100, 10, 0, 0)
        assertEquals(Direction.RIGHT, handler.getDirection())

        handler.reset()
        handler.touchDown(0, 0, 0, 0)
        handler.touchUp(10, 100, 0, 0)
        assertEquals(Direction.DOWN, handler.getDirection())
    }

    @Test
    fun `новое нажатие клавиатуры перебивает свайп`() {
        handler.touchDown(0, 0, 0, 0)
        handler.touchUp(100, 0, 0, 0)
        assertEquals(Direction.RIGHT, handler.getDirection())

        fakeInput.pressKey(Input.Keys.LEFT)
        assertEquals(Direction.LEFT, handler.getDirection())
    }

    @Test
    fun `зажатая клавиша не перебивает свайп каждый кадр`() {
        fakeInput.pressKey(Input.Keys.UP)
        assertEquals(Direction.UP, handler.getDirection())

        handler.touchDown(0, 0, 0, 0)
        handler.touchUp(0, 100, 0, 0)
        // Клавиша всё ещё зажата, но не менялась — свайп сохраняется
        assertEquals(Direction.DOWN, handler.getDirection())
    }

    @Test
    fun `reset сбрасывает ввод`() {
        handler.setDirection(Direction.LEFT)
        assertEquals(Direction.LEFT, handler.getLastInputDirection())

        handler.reset()

        assertEquals(Direction.NONE, handler.getLastInputDirection())
        assertEquals(Direction.NONE, handler.getDirection())
    }

    @Test
    fun `cancelTouch отменяет активный свайп`() {
        handler.touchDown(0, 0, 0, 0)
        handler.cancelTouch()
        assertFalse(handler.touchUp(100, 0, 0, 0))
        assertEquals(Direction.NONE, handler.getDirection())
    }

    @Test
    fun `togglePause даёт однократный запрос паузы`() {
        handler.togglePause()
        assertTrue(handler.isPauseJustPressed())
        assertFalse(handler.isPauseJustPressed())
    }

    @Test
    fun `пробел даёт запрос паузы`() {
        fakeInput.tapKey(Input.Keys.SPACE)
        assertTrue(handler.isPauseJustPressed())
    }

    @Test
    fun `setDirection задаёт направление как свайп`() {
        handler.setDirection(Direction.LEFT)
        assertEquals(Direction.LEFT, handler.getDirection())
        assertEquals(Direction.LEFT, handler.getLastInputDirection())
    }

    @Test
    fun `setDirection с NONE игнорируется`() {
        handler.setDirection(Direction.NONE)
        assertEquals(Direction.NONE, handler.getLastInputDirection())
    }
}
