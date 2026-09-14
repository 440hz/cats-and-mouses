package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

/**
 * Конфиг темы. Читается из <assets>/themes/<id>/theme.json.
 * wallFiles[0] — основной спрайт стены, остальные (wall_1..N) — дополнительные.
 * Парсинг через встроенный каркас LibGDX, без сторонних библиотек.
 */
class ThemeLoader(indexPath: String = "themes/themes.json") {

    private val indexJson: JsonValue = JsonReader().parse(Gdx.files.internal(indexPath))
    private val themeJson: JsonValue

    /** Идентификатор активной темы — имя папки в assets/themes/. */
    val activeTheme: String = indexJson.getString("active")

    /** Название активной темы из её theme.json (для HUD/логов). */
    val name: String
        get() = themeJson.getString("name")

    /** Пути к спрайтам стен: index 0 — основной, остальные — допы. */
    val wallPaths: List<String>
        get() = themeJson.get("wallFiles").map { "$themeDir/${it.asString()}" }

    /** Минимум случайных дополнительных стен на карту. */
    val extrasCountMin: Int
        get() = themeJson.getInt("extrasCountMin")

    /** Максимум случайных дополнительных стен на карту. */
    val extrasCountMax: Int
        get() = themeJson.getInt("extrasCountMax")

    /** Путь к фоновому спрайту темы. */
    val backgroundPath: String
        get() = "$themeDir/${themeJson.getString("background")}"

    private val themeDir: String
        get() = "themes/$activeTheme"

    init {
        themeJson = JsonReader().parse(Gdx.files.internal("$themeDir/theme.json"))
    }
}