package com.catsandmouses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EditorAssetsTest {

    @TempDir
    lateinit var tmp: File

    private fun dirWithMaps(name: String): File {
        val dir = File(tmp, name)
        File(dir, "maps").mkdirs()
        return dir.absoluteFile.normalize()
    }

    @Test
    fun `приоритет у --assets-dir`() {
        val arg = dirWithMaps("arg")
        val sys = dirWithMaps("sys")
        assertEquals(arg, EditorFiles.resolveAssetsDir(arg.path, sys.path, tmp))
    }

    @Test
    fun `системное свойство когда аргумент не задан`() {
        val sys = dirWithMaps("sys")
        assertEquals(sys, EditorFiles.resolveAssetsDir(null, sys.path, tmp))
        assertEquals(sys, EditorFiles.resolveAssetsDir("  ", sys.path, tmp))
    }

    @Test
    fun `фолбэк на assets рядом с рабочей папкой`() {
        dirWithMaps("assets")
        val work = File(tmp, "lwjgl3").apply { mkdirs() }
        assertEquals(File(tmp, "assets").absoluteFile.normalize(), EditorFiles.resolveAssetsDir(null, null, work))
    }

    @Test
    fun `read-only когда папка не найдена`() {
        val empty = File(tmp, "nowhere").apply { mkdirs() }
        assertNull(EditorFiles.resolveAssetsDir(null, null, empty))
    }

    @Test
    fun `несуществующий явный путь не выбирается`() {
        val empty = File(tmp, "nowhere").apply { mkdirs() }
        assertNull(EditorFiles.resolveAssetsDir(File(tmp, "missing").path, null, empty))
    }

    @Test
    fun `saveMap пишет и перезаписывает без временного файла`() {
        val assets = File(tmp, "assets").apply { mkdirs() }
        val target = File(assets, "maps/1/test.txt")

        EditorFiles.saveMap(assets, "maps/1/test.txt", "первый\n")
        assertEquals("первый\n", target.readText())

        EditorFiles.saveMap(assets, "maps/1/test.txt", "второй\n")
        assertEquals("второй\n", target.readText())
        assertFalse(File(target.parentFile, "test.txt.tmp").exists(), "временный файл должен быть удалён")
    }

    @Test
    fun `кандидаты идут в порядке приоритета`() {
        val candidates = EditorFiles.assetsDirCandidates("/a", "/b", File("/work"))
        assertEquals(File("/a").absoluteFile.normalize(), candidates[0])
        assertEquals(File("/b").absoluteFile.normalize(), candidates[1])
        assertEquals(File(File("/work", "../assets").absoluteFile.normalize().path), candidates[2].absoluteFile.normalize())
    }
}
