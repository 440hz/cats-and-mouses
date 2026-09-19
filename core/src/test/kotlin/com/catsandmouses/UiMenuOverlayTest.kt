package com.catsandmouses

import com.badlogic.gdx.math.Rectangle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UiMenuOverlayTest {

    private val actions = mutableListOf<String>()

    private fun overlay(buttonCount: Int = 3): UiMenuOverlay {
        val buttons = (0 until buttonCount).map { index ->
            UiButton(
                bounds = Rectangle(0f, (200 - index * 60).toFloat(), 100f, 50f),
                text = "B$index",
                font = TestFakes.font(),
                onClick = { actions.add("B$index") }
            )
        }
        return UiMenuOverlay(title = "Заголовок", buttons = buttons, titleFont = TestFakes.font())
    }

    @Test
    fun `фокус по умолчанию на первой кнопке`() {
        assertEquals(0, overlay().focusedIndex)
    }

    @Test
    fun `moveFocus двигает фокус вперёд`() {
        val overlay = overlay()
        overlay.moveFocus(1)
        assertEquals(1, overlay.focusedIndex)
    }

    @Test
    fun `moveFocus зацикливается вперёд`() {
        val overlay = overlay()
        overlay.moveFocus(1)
        overlay.moveFocus(1)
        overlay.moveFocus(1)
        assertEquals(0, overlay.focusedIndex)
    }

    @Test
    fun `moveFocus зацикливается назад`() {
        val overlay = overlay()
        overlay.moveFocus(-1)
        assertEquals(2, overlay.focusedIndex)
    }

    @Test
    fun `moveFocus на пустом списке безопасен`() {
        val overlay = overlay(buttonCount = 0)
        overlay.moveFocus(1)
        overlay.moveFocus(-1)
        assertEquals(0, overlay.focusedIndex)
    }

    @Test
    fun `triggerFocused активирует кнопку под фокусом`() {
        val overlay = overlay()
        overlay.focusedIndex = 1
        overlay.triggerFocused()
        assertEquals(listOf("B1"), actions)
    }

    @Test
    fun `triggerFocused на пустом списке безопасен`() {
        overlay(buttonCount = 0).triggerFocused()
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `triggerFocused вне диапазона безопасен`() {
        val overlay = overlay()
        overlay.focusedIndex = 99
        overlay.triggerFocused()
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `touchDown попадает в кнопку и переводит на неё фокус`() {
        val overlay = overlay()
        val handled = overlay.touchDown(50f, 225f)

        assertTrue(handled)
        assertEquals(0, overlay.focusedIndex)
        assertTrue(overlay.buttons[0].isPressed)
    }

    @Test
    fun `touchDown переводит фокус на нажатую кнопку`() {
        val overlay = overlay()
        overlay.touchDown(50f, 165f)
        assertEquals(1, overlay.focusedIndex)
    }

    @Test
    fun `touchDown вне кнопок не перехватывается`() {
        val overlay = overlay()
        overlay.focusedIndex = 1
        assertFalse(overlay.touchDown(500f, 500f))
        assertEquals(1, overlay.focusedIndex)
    }

    @Test
    fun `touchUp внутри активирует кнопку`() {
        val overlay = overlay()
        overlay.touchDown(50f, 225f)
        assertTrue(overlay.touchUp(50f, 225f))
        assertEquals(listOf("B0"), actions)
    }

    @Test
    fun `click снаружи отменяется`() {
        val overlay = overlay()
        overlay.touchDown(50f, 225f)
        assertFalse(overlay.touchUp(500f, 500f))
        assertTrue(actions.isEmpty())
    }
}
