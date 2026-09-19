package com.catsandmouses

import com.badlogic.gdx.Gdx
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LaunchModeTest {

    private val saved = System.getProperty("launch.mode")

    @AfterEach
    fun restore() {
        if (saved == null) System.clearProperty("launch.mode") else System.setProperty("launch.mode", saved)
        Gdx.app = null
    }

    @Test
    fun `явное значение launch mode даёт суффикс в скобках`() {
        System.setProperty("launch.mode", "i")
        assertEquals("(i)", LaunchMode.suffix)
    }

    @Test
    fun `пустое значение launch mode игнорируется`() {
        Gdx.app = null
        System.setProperty("launch.mode", "   ")
        // Пустое значение не принимается, поэтому режим вычисляется: не Android и не jar → dev
        assertEquals("(d)", LaunchMode.suffix)
    }

    @Test
    fun `без свойства и без Android определяется как dev`() {
        Gdx.app = null
        System.clearProperty("launch.mode")
        // Тестовые классы лежат не в jar → dev-режим
        assertEquals("(d)", LaunchMode.suffix)
    }
}
