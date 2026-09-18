package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.JsonReader

/**
 * Каталог уровней прохождения. Читает сгенерированный манифест assets/maps/maps.json,
 * где уровни идут в порядке: миры по номеру → карты внутри мира → final последним.
 * Для каждого уровня хранится путь, тема мира и признак финала.
 */
class LevelCatalog(manifestPath: String = Constants.MAPS_MANIFEST) {

    data class Level(val path: String, val themeId: String, val isFinal: Boolean)

    val levels: List<Level>

    val count: Int
        get() = levels.size

    init {
        val file = Gdx.files.internal(manifestPath)
        levels = if (file.exists()) parse(file.readString()) else fallback()
    }

    private fun parse(text: String): List<Level> {
        val json = JsonReader().parse(text)
        val array = json.get("levels") ?: return fallback()
        val parsed = array.map { item ->
            Level(
                path = item.getString("path"),
                themeId = item.getString("theme", "system"),
                isFinal = item.getBoolean("final", false)
            )
        }
        return if (parsed.isEmpty()) fallback() else parsed
    }

    private fun fallback(): List<Level> {
        Gdx.app.log("LevelCatalog", "maps.json missing/empty, fallback to maps/final.txt")
        return listOf(Level("maps/final.txt", "system", true))
    }

    /** Индекс уровня по имени карты (`1/2`, `final`, `maps/1/2.txt`). */
    fun indexOfMap(mapName: String): Int {
        val path = normalize(mapName)
        val index = levels.indexOfFirst { it.path == path }
        return if (index >= 0) index else 0
    }

    /** Имя карты для индекса уровня (`1/2`, `final`). */
    fun mapName(index: Int): String =
        levels[index.coerceIn(0, count - 1)].path.removePrefix("maps/").removeSuffix(".txt")

    /** Тема мира для индекса уровня. */
    fun themeFor(index: Int): String = levels[index.coerceIn(0, count - 1)].themeId

    /** Финальный ли уровень. */
    fun isFinal(index: Int): Boolean = levels[index.coerceIn(0, count - 1)].isFinal

    private fun normalize(mapName: String): String {
        val core = mapName.removePrefix("maps/").removeSuffix(".txt")
        return "maps/$core.txt"
    }
}
