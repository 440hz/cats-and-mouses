package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessFiles
import com.badlogic.gdx.utils.GdxRuntimeException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import java.io.File

/**
 * База для тестов, которым нужен минимальный LibGDX-контекст без окна и рендера:
 * [Gdx.files] (чтение реальных assets), [Gdx.app] (логи) и [Gdx.input] ([FakeInput]).
 *
 * Рабочая папка тестов — `assets` (см. core/build.gradle.kts), поэтому
 * `Gdx.files.internal("maps/1/1.txt")` читает настоящие карты.
 */
abstract class GdxTestBase {

    protected lateinit var fakeInput: FakeInput

    @BeforeEach
    fun setUpGdx() {
        Gdx.files = HeadlessFiles()
        Gdx.app = FakeApplication()
        fakeInput = FakeInput()
        Gdx.input = fakeInput
    }

    @AfterEach
    fun tearDownGdx() {
        Gdx.input = null
        Gdx.app = null
        Gdx.files = null
    }

    companion object {

        /** Корень проекта (задаётся Gradle-свойством `project.root`). */
        private val projectRoot: File
            get() {
                val configured = System.getProperty("project.root")
                if (configured != null) return File(configured)
                // Запасной вариант: рабочая папка тестов — <project>/assets
                return File(System.getProperty("user.dir")).absoluteFile.parentFile
            }

        /** Пропускает тест, если файла нет (например, не сгенерирован maps.json). */
        fun assumeProjectFile(relativePath: String) {
            assumeTrue(File(projectRoot, relativePath).exists(), "Нет файла $relativePath")
        }

        /** Читает текст файла из корня проекта. */
        fun readProjectFile(relativePath: String): String {
            val file = File(projectRoot, relativePath)
            if (!file.exists()) throw GdxRuntimeException("Нет файла: ${file.absolutePath}")
            return file.readText()
        }
    }
}
