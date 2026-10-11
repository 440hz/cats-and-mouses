package com.catsandmouses

import com.badlogic.gdx.Gdx
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MePhotosTest : GdxTestBase() {

    @Test
    fun `манифест фото существует и содержит пути me`() {
        assumeProjectFile("assets/${MePhotos.MANIFEST}")
        val paths = MePhotos.paths()
        assertTrue(paths.isNotEmpty(), "в манифесте должно быть хотя бы одно фото")
        assertTrue(paths.all { it.startsWith("me/") }, "пути должны быть вида me/<файл>")
        assertTrue(Gdx.files.internal(paths.first()).exists(), "первое фото должно существовать")
    }
}
