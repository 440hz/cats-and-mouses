package com.catsandmouses

import com.badlogic.gdx.backends.headless.mock.input.MockInput

/**
 * Подменяемый ввод для тестов: управляемые нажатые и «только что нажатые» клавиши.
 * Остальное наследуется от no-op [MockInput].
 */
class FakeInput : MockInput() {

    private val pressedKeys = mutableSetOf<Int>()
    private val justPressedKeys = mutableSetOf<Int>()

    override fun isKeyPressed(key: Int): Boolean = key in pressedKeys

    override fun isKeyJustPressed(key: Int): Boolean = key in justPressedKeys

    /** Клавиша удерживается. */
    fun pressKey(key: Int) {
        pressedKeys.add(key)
    }

    /** Клавиша отпущена. */
    fun releaseKey(key: Int) {
        pressedKeys.remove(key)
    }

    /** Клавиша «только что нажата» (один кадр). */
    fun tapKey(key: Int) {
        justPressedKeys.add(key)
    }

    /** Отпустить все клавиши и сбросить однокадровые нажатия. */
    fun clearAll() {
        pressedKeys.clear()
        justPressedKeys.clear()
    }
}
