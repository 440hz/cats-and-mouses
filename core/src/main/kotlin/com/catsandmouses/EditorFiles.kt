package com.catsandmouses

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Файловые операции редактора карт: разрешение папки `assets` и безопасная запись.
 * Используется только Desktop-редактором (в APK не задействован).
 *
 * `Gdx.files.internal` доступен только на чтение (ассеты в jar/APK), а
 * `Gdx.files.local` зависит от рабочей папки, поэтому путь к исходным `assets`
 * определяется явно по приоритету кандидатов.
 */
object EditorFiles {

    /**
     * Кандидаты папки assets в порядке приоритета (без проверки существования):
     * явный аргумент `--assets-dir` → системное свойство `assets.dir` →
     * `../assets` → `assets` относительно рабочей папки.
     */
    fun assetsDirCandidates(argPath: String?, systemProperty: String?, workingDir: File): List<File> {
        val result = mutableListOf<File>()
        argPath?.takeIf { it.isNotBlank() }?.let { result.add(File(it).absoluteFile.normalize()) }
        systemProperty?.takeIf { it.isNotBlank() }?.let { result.add(File(it).absoluteFile.normalize()) }
        result.add(File(workingDir, "../assets").absoluteFile.normalize())
        result.add(File(workingDir, "assets").absoluteFile.normalize())
        return result.distinct()
    }

    /** Найти существующую папку assets среди кандидатов (признак — вложенная папка `maps`). */
    fun resolveAssetsDir(argPath: String?, systemProperty: String?, workingDir: File): File? =
        assetsDirCandidates(argPath, systemProperty, workingDir).firstOrNull { File(it, "maps").isDirectory }

    /** Безопасная запись карты: во временный файл рядом, затем замена целевого. */
    fun saveMap(assetsDir: File, relativePath: String, text: String) {
        val target = File(assetsDir, relativePath)
        val parent = target.parentFile
            ?: throw IllegalArgumentException("Нет родительской папки для $relativePath")
        parent.mkdirs()

        val tmp = File(parent, target.name + ".tmp")
        tmp.writeText(text)

        try {
            Files.move(
                tmp.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: Exception) {
            // ATOMIC_MOVE может не поддерживаться на некоторых ФС — запасной путь
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                tmp.delete()
                throw e
            }
        }
    }
}
