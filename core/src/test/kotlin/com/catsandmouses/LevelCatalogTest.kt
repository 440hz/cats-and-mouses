package com.catsandmouses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LevelCatalogTest : GdxTestBase() {

    private fun catalog(path: String) = LevelCatalog(path)

    @Test
    fun `кастомный манифест парсится с сохранением порядка`() {
        val catalog = catalog("maps/catalog_ok.json")
        assertEquals(3, catalog.count)
        assertEquals(listOf("maps/a.txt", "maps/b.txt", "maps/final.txt"), catalog.levels.map { it.path })
    }

    @Test
    fun `темы и финал читаются из манифеста`() {
        val catalog = catalog("maps/catalog_ok.json")
        assertEquals("grass", catalog.themeFor(0))
        assertEquals("ice", catalog.themeFor(1))
        assertEquals("space", catalog.themeFor(2))
        assertFalse(catalog.isFinal(0))
        assertFalse(catalog.isFinal(1))
        assertTrue(catalog.isFinal(2))
    }

    @Test
    fun `отсутствующие поля получают значения по умолчанию`() {
        val catalog = catalog("maps/catalog_notheme.json")
        assertEquals(1, catalog.count)
        assertEquals("system", catalog.themeFor(0))
        assertFalse(catalog.isFinal(0))
    }

    @Test
    fun `indexOfMap нормализует имя карты`() {
        val catalog = catalog("maps/catalog_ok.json")
        assertEquals(0, catalog.indexOfMap("a"))
        assertEquals(1, catalog.indexOfMap("b"))
        assertEquals(1, catalog.indexOfMap("maps/b.txt"))
        assertEquals(2, catalog.indexOfMap("final"))
        assertEquals(0, catalog.indexOfMap("maps/unknown.txt"))
    }

    @Test
    fun `mapName возвращает путь без префикса и расширения`() {
        val catalog = catalog("maps/catalog_ok.json")
        assertEquals("a", catalog.mapName(0))
        assertEquals("b", catalog.mapName(1))
        assertEquals("final", catalog.mapName(2))
    }

    @Test
    fun `индексы клампятся по границам`() {
        val catalog = catalog("maps/catalog_ok.json")
        assertEquals("a", catalog.mapName(-5))
        assertEquals("final", catalog.mapName(99))
        assertEquals("grass", catalog.themeFor(-5))
        assertEquals("space", catalog.themeFor(99))
        assertFalse(catalog.isFinal(-5))
        assertTrue(catalog.isFinal(99))
    }

    @Test
    fun `пустой список уровней даёт fallback на final`() {
        assertFallback(catalog("maps/catalog_empty.json"))
    }

    @Test
    fun `манифест без поля levels даёт fallback`() {
        assertFallback(catalog("maps/catalog_nolevels.json"))
    }

    @Test
    fun `отсутствующий манифест даёт fallback`() {
        assertFallback(catalog("maps/does_not_exist_manifest.json"))
    }

    @Test
    fun `реальный манифест соответствует ассетам`() {
        assumeProjectFile("assets/maps/maps.json")
        val catalog = catalog("maps/maps.json")

        assertTrue(catalog.count >= 2, "должно быть минимум 2 уровня, есть ${catalog.count}")
        assertTrue(catalog.levels.all { it.path.matches(Regex("""maps/.+\.txt""")) }, "все пути — .txt в maps/")
        assertTrue(catalog.levels.all { it.themeId.isNotBlank() })

        // final идёт последним и ровно один
        assertEquals(1, catalog.levels.count { it.isFinal })
        assertTrue(catalog.isFinal(catalog.count - 1))
        assertFalse(catalog.levels.dropLast(1).any { it.isFinal })

        // Первый уровень достижим по своему имени
        assertEquals(0, catalog.indexOfMap(catalog.mapName(0)))
    }

    private fun assertFallback(catalog: LevelCatalog) {
        assertEquals(1, catalog.count)
        assertEquals("maps/final.txt", catalog.levels[0].path)
        assertEquals("system", catalog.themeFor(0))
        assertTrue(catalog.isFinal(0))
        assertEquals("final", catalog.mapName(0))
    }
}
