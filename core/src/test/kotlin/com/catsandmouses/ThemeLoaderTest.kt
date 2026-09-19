package com.catsandmouses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThemeLoaderTest : GdxTestBase() {

    @Test
    fun `exists определяет наличие темы`() {
        assumeProjectFile("assets/themes/themes.json")
        assertTrue(ThemeLoader.exists("grass"))
        assertTrue(ThemeLoader.exists("testplain"))
        assertFalse(ThemeLoader.exists("no_such_theme"))
    }

    @Test
    fun `systemId ссылается на существующую тему`() {
        assumeProjectFile("assets/themes/themes.json")
        val id = ThemeLoader.systemId()
        assertTrue(id.isNotBlank())
        assertTrue(ThemeLoader.exists(id), "системная тема $id должна существовать")
    }

    @Test
    fun `system загружает системную тему`() {
        assumeProjectFile("assets/themes/themes.json")
        val theme = ThemeLoader.system()
        assertEquals(ThemeLoader.systemId(), theme.activeTheme)
        assertTrue(theme.name.isNotBlank())
    }

    @Test
    fun `тема с autotile читает параметры атласа`() {
        assumeProjectFile("assets/themes/grass/theme.json")
        val theme = ThemeLoader.forId("grass")
        assertEquals("grass", theme.activeTheme)
        assertTrue(theme.hasAutotile)
        assertEquals("themes/grass/wall_autotile.png", theme.wallAutotilePath)
        assertEquals(40, theme.tileSize)
        assertEquals(4, theme.atlasColumns)
        assertEquals(4, theme.atlasRows)
        assertTrue(theme.backgroundPath.startsWith("themes/grass/"))
        assertTrue(theme.extrasCountMin <= theme.extrasCountMax)
    }

    @Test
    fun `тема без autotile читает список стен`() {
        val theme = ThemeLoader.forId("testplain")
        assertEquals("testplain", theme.activeTheme)
        assertFalse(theme.hasAutotile)
        assertEquals(
            listOf("themes/testplain/wall_0.png", "themes/testplain/wall_1.png"),
            theme.wallPaths
        )
        assertEquals("themes/testplain/background.png", theme.backgroundPath)
        assertEquals(2, theme.extrasCountMin)
        assertEquals(4, theme.extrasCountMax)
        // Поля autotile отсутствуют — нули/null
        assertEquals(0, theme.tileSize)
        assertEquals(0, theme.atlasColumns)
        assertEquals(0, theme.atlasRows)
    }

    @Test
    fun `forId на неизвестную тему даёт системную`() {
        assumeProjectFile("assets/themes/themes.json")
        val system = ThemeLoader.system()
        assertEquals(system.activeTheme, ThemeLoader.forId("no_such_theme").activeTheme)
    }

    @Test
    fun `forId со system null и пустой строкой даёт системную`() {
        assumeProjectFile("assets/themes/themes.json")
        val system = ThemeLoader.system()
        assertEquals(system.activeTheme, ThemeLoader.forId("system").activeTheme)
        assertEquals(system.activeTheme, ThemeLoader.forId(null).activeTheme)
        assertEquals(system.activeTheme, ThemeLoader.forId("").activeTheme)
        assertEquals(system.activeTheme, ThemeLoader.forId("   ").activeTheme)
    }
}
