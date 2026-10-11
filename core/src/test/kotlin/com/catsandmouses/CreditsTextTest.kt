package com.catsandmouses

import com.catsandmouses.i18n.Lang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreditsTextTest : GdxTestBase() {

    @Test
    fun `путь к файлу титров зависит от языка`() {
        assertEquals("text/credits_ru.txt", CreditsText.pathFor(Lang.RU))
        assertEquals("text/credits_en.txt", CreditsText.pathFor(Lang.EN))
    }

    @Test
    fun `файлы титров существуют и непустые`() {
        for (lang in Lang.entries) {
            val path = CreditsText.pathFor(lang)
            assumeProjectFile("assets/$path")
            val lines = CreditsText.lines(lang)
            assertTrue(lines.size >= 5, "титры ${lang.code} должны содержать строки")
            assertTrue(lines.any { it.isNotBlank() }, "титры ${lang.code} не должны быть пустыми")
        }
    }
}
