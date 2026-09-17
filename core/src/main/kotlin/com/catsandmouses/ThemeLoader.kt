package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

/**
 * Конфиг темы. Читается из <assets>/themes/<id>/theme.json.
 * wallFiles[0] — основной спрайт стены, остальные (wall_1..N) — дополнительные.
 * wallAutotile — опциональный 15-piece dual-grid атлас стен;
 * если задан, стены рисуются через dual-grid, а wallFiles/extras игнорируются.
 * overrideTheme — id темы из параметра запуска `--theme`; имеет приоритет над
 * полем `active` в themes.json. Если такой темы нет — откат на `active`.
 * Парсинг через встроенный каркас LibGDX, без сторонних библиотек.
 */
class ThemeLoader(
    indexPath: String = "themes/themes.json",
    overrideTheme: String? = null
) {

    private val indexJson: JsonValue = JsonReader().parse(Gdx.files.internal(indexPath))
    private val themeJson: JsonValue

    /** Идентификатор активной темы — имя папки в assets/themes/. */
    val activeTheme: String = resolveTheme(indexJson, overrideTheme)

    /** Название активной темы из её theme.json (для HUD/логов). */
    val name: String
        get() = themeJson.getString("name")

    /** Пути к спрайтам стен: index 0 — основной, остальные — допы. */
    val wallPaths: List<String>
        get() = themeJson.get("wallFiles").map { "$themeDir/${it.asString()}" }

    /** Путь к 15-piece dual-grid атласу стен или null, если не задан. */
    val wallAutotilePath: String?
        get() = themeJson.get("wallAutotile")?.let { "$themeDir/${it.asString()}" }

    /** Размер одного тайла в атласе (только для autotile). */
    val tileSize: Int
        get() = themeJson.get("tileSize")?.asInt() ?: 0

    /** Число колонок в атласе (только для autotile). */
    val atlasColumns: Int
        get() = themeJson.get("atlasColumns")?.asInt() ?: 0

    /** Число строк в атласе (только для autotile). */
    val atlasRows: Int
        get() = themeJson.get("atlasRows")?.asInt() ?: 0

    /** true, если тема использует dual-grid autotile для стен. */
    val hasAutotile: Boolean
        get() = wallAutotilePath != null

    /** Минимум случайных дополнительных стен на карту (только для wallFiles). */
    val extrasCountMin: Int
        get() = themeJson.getInt("extrasCountMin")

    /** Максимум случайных дополнительных стен на карту (только для wallFiles). */
    val extrasCountMax: Int
        get() = themeJson.getInt("extrasCountMax")

    /** Путь к фоновому спрайту темы. */
    val backgroundPath: String
        get() = "$themeDir/${themeJson.getString("background")}"

    /** Приоритет: `--theme`, затем `active` из themes.json; неизвестная тема → откат на `active`. */
    private fun resolveTheme(index: JsonValue, override: String?): String {
        val active = index.getString("active")
        if (override.isNullOrBlank()) return active
        return if (Gdx.files.internal("themes/$override/theme.json").exists()) {
            override
        } else {
            Gdx.app.log("ThemeLoader", "Theme '$override' not found, fallback to '$active'")
            active
        }
    }

    private val themeDir: String
        get() = "themes/$activeTheme"

    init {
        themeJson = JsonReader().parse(Gdx.files.internal("$themeDir/theme.json"))
    }
}