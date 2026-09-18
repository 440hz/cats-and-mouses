package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

/**
 * Конфиг темы. Читается из <assets>/themes/<id>/theme.json.
 * wallFiles[0] — основной спрайт стены, остальные (wall_1..N) — дополнительные.
 * wallAutotile — опциональный 15-piece dual-grid атлас стен;
 * если задан, стены рисуются через dual-grid, а wallFiles/extras игнорируются.
 *
 * Системная тема (заставка, меню, оверлеи) задаётся полем `system` в themes.json
 * и создаётся через [system]. Тема конкретного мира/уровня — через [forId]
 * (если темы нет — откат на системную). Парсинг через встроенный каркас LibGDX.
 */
class ThemeLoader private constructor(
    /** Идентификатор темы — имя папки в assets/themes/. */
    val activeTheme: String,
    private val themeJson: JsonValue
) {

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

    private val themeDir: String
        get() = "themes/$activeTheme"

    companion object {
        private const val INDEX_PATH = "themes/themes.json"

        /** Id системной темы: поле `system`, иначе `active`, иначе первая из списка. */
        fun systemId(): String {
            val index = JsonReader().parse(Gdx.files.internal(INDEX_PATH))
            val candidate = index.get("system")?.asString()?.takeIf { it.isNotBlank() }
                ?: index.get("active")?.asString()?.takeIf { it.isNotBlank() }
            if (candidate != null && exists(candidate)) return candidate
            val first = index.get("themes")?.firstOrNull()?.getString("id")
            if (first != null && exists(first)) return first
            return candidate ?: "grass"
        }

        /** Есть ли тема с таким id (папка с theme.json). */
        fun exists(id: String): Boolean = Gdx.files.internal("themes/$id/theme.json").exists()

        /** Системная тема (заставка, меню, оверлеи). */
        fun system(): ThemeLoader = load(systemId())

        /** Тема по id с откатом на системную, если темы нет. */
        fun forId(id: String?): ThemeLoader =
            if (!id.isNullOrBlank() && id != "system" && exists(id)) load(id) else system()

        private fun load(id: String): ThemeLoader {
            val json = JsonReader().parse(Gdx.files.internal("themes/$id/theme.json"))
            return ThemeLoader(id, json)
        }
    }
}
