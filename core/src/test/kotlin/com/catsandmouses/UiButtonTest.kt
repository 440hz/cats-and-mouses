package com.catsandmouses

import com.badlogic.gdx.math.Rectangle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UiButtonTest {

    private var clicks = 0

    private fun button(): UiButton = UiButton(
        bounds = Rectangle(100f, 100f, 200f, 50f),
        text = "Тест",
        font = TestFakes.font(),
        onClick = { clicks++ }
    )

    @Test
    fun `hit внутри границ`() {
        val button = button()
        assertTrue(button.hit(150f, 125f))
    }

    @Test
    fun `hit на границе включается`() {
        val button = button()
        assertTrue(button.hit(100f, 100f))
        assertTrue(button.hit(300f, 150f))
    }

    @Test
    fun `hit снаружи`() {
        val button = button()
        assertFalse(button.hit(99f, 125f))
        assertFalse(button.hit(150f, 151f))
        assertFalse(button.hit(301f, 125f))
    }

    @Test
    fun `клик внутри кнопки вызывает действие`() {
        val button = button()
        assertTrue(button.touchDown(150f, 125f))
        assertTrue(button.isPressed)
        assertTrue(button.touchUp(150f, 125f))
        assertEquals(1, clicks)
        assertFalse(button.isPressed)
    }

    @Test
    fun `отпускание снаружи не вызывает действие`() {
        val button = button()
        button.touchDown(150f, 125f)
        assertFalse(button.touchUp(50f, 50f))
        assertEquals(0, clicks)
        assertFalse(button.isPressed)
    }

    @Test
    fun `touchDown снаружи не нажимает кнопку`() {
        val button = button()
        assertFalse(button.touchDown(0f, 0f))
        assertFalse(button.isPressed)
    }

    @Test
    fun `touchUp без touchDown не вызывает действие`() {
        val button = button()
        assertFalse(button.touchUp(150f, 125f))
        assertEquals(0, clicks)
    }

    @Test
    fun `triggerClick вызывает действие и сбрасывает нажатие`() {
        val button = button()
        button.triggerClick()
        assertEquals(1, clicks)
        assertFalse(button.isPressed)
    }

    @Test
    fun `сдвиг активной кнопки равен 10`() {
        assertEquals(10f, UiButton.LIFT)
    }
}
