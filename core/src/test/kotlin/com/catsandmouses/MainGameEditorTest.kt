package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MainGameEditorTest : GdxTestBase() {

    private val fakeScreen = object : Screen {
        override fun show() {}
        override fun render(delta: Float) {}
        override fun resize(width: Int, height: Int) {}
        override fun pause() {}
        override fun resume() {}
        override fun hide() {}
        override fun dispose() {}
    }

    @BeforeEach
    fun provideGraphics() {
        // Game.setScreen обращается к Gdx.graphics — headless-мок отдаёт размеры 0
        Gdx.graphics = MockGraphics()
    }

    @AfterEach
    fun clearGraphics() {
        Gdx.graphics = null
    }

    @Test
    fun `editorFactory ставит переданный экран без меню`() {
        val game = MainGame(editorFactory = { fakeScreen })
        game.create()
        assertSame(fakeScreen, game.getScreen())
    }
}
