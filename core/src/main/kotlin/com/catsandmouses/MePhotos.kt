package com.catsandmouses

import com.badlogic.gdx.Gdx

/**
 * Список фото пасхалки (карусель после финальных титров) — из `assets/me/photos.json`.
 * Манифест генерируется Gradle-задачей `generateMePhotosManifest` (каталог в jar/APK не листается).
 */
object MePhotos {

    const val MANIFEST = "me/photos.json"

    /** Пути к фото в порядке манифеста (напр. `me/1.jpg`); пусто, если манифеста нет. */
    fun paths(): List<String> {
        val file = Gdx.files.internal(MANIFEST)
        if (!file.exists()) return emptyList()
        val text = file.readString("UTF-8")
        return Regex("\"([^\"]+\\.(?:jpg|jpeg|png))\"", RegexOption.IGNORE_CASE)
            .findAll(text)
            .map { "me/${it.groupValues[1]}" }
            .toList()
    }
}
