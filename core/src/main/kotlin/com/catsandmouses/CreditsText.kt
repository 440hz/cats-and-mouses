package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.catsandmouses.i18n.I18n
import com.catsandmouses.i18n.Lang
import com.catsandmouses.i18n.Str

/**
 * Текст финальных титров — из файла `assets/text/credits_<lang>.txt` (RU/EN).
 * Правится без сборки кода; если файла нет — показывается название игры.
 */
object CreditsText {

    /** Путь к файлу титров для языка (`text/credits_ru.txt` / `text/credits_en.txt`). */
    fun pathFor(lang: Lang): String = "text/credits_${lang.code}.txt"

    /** Строки титров для текущего языка [I18n.lang]. */
    fun lines(lang: Lang = I18n.lang): List<String> {
        val file = Gdx.files.internal(pathFor(lang))
        if (!file.exists()) return listOf(I18n.t(Str.TITLE_GAME))
        return file.readString("UTF-8")
            .replace("\r\n", "\n")
            .split("\n")
    }
}
