package com.catsandmouses

import com.catsandmouses.i18n.I18n
import com.catsandmouses.i18n.Lang
import com.catsandmouses.i18n.Str
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class I18nTest {

    @BeforeEach
    fun setRussian() {
        I18n.lang = Lang.RU
    }

    @AfterEach
    fun resetLanguage() {
        I18n.lang = Lang.RU
    }

    @Test
    fun `словари полны для обоих языков`() {
        Str.entries.forEach { key ->
            Lang.entries.forEach { lang ->
                I18n.lang = lang
                val text = I18n.t(key)
                assertTrue(text.isNotBlank(), "пустой текст для $key ($lang)")
                assertNotEquals(key.name, text, "нет перевода для $key ($lang)")
            }
        }
    }

    @Test
    fun `ключевые строки различаются между языками`() {
        assertEquals("Меню", I18n.t(Str.TITLE_MENU))
        I18n.lang = Lang.EN
        assertEquals("Menu", I18n.t(Str.TITLE_MENU))
        assertNotEquals(I18n.t(Str.TITLE_GAME), I18n.t(Str.TITLE_MENU))
    }

    @Test
    fun `форматирование HUD подставляет значения`() {
        I18n.lang = Lang.RU
        assertEquals("Сыр: 3/9", I18n.t(Str.HUD_CHEESE, 3, 9))
        assertEquals("Карта 2/36", I18n.t(Str.HUD_MAP, 2, 36))
        I18n.lang = Lang.EN
        assertEquals("Cheese: 3/9", I18n.t(Str.HUD_CHEESE, 3, 9))
        assertEquals("Level 2/36", I18n.t(Str.HUD_MAP, 2, 36))
    }

    @Test
    fun `toggle переключает язык`() {
        assertEquals(Lang.RU, I18n.lang)
        assertEquals(Lang.EN, I18n.toggle())
        assertEquals(Lang.RU, I18n.toggle())
    }

    @Test
    fun `fromCode распознаёт ru и en и откатывается на дефолт`() {
        assertEquals(Lang.RU, Lang.fromCode("ru"))
        assertEquals(Lang.EN, Lang.fromCode("en"))
        assertEquals(Lang.EN, Lang.fromCode("EN"))
        assertEquals(Lang.DEFAULT, Lang.fromCode("fr"))
        assertEquals(Lang.DEFAULT, Lang.fromCode(null))
        assertEquals(Lang.DEFAULT, Lang.fromCode(""))
    }

    @Test
    fun `system возвращает поддерживаемый язык`() {
        assertTrue(Lang.system() in Lang.entries)
    }
}
