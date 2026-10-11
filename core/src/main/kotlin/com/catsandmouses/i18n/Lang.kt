package com.catsandmouses.i18n

import java.util.Locale

/**
 * Язык интерфейса игры. Сейчас поддерживаются русский (RU) и английский (EN).
 * Строки — в [I18n]; выбор языка хранит `ProgressStore`.
 */
enum class Lang(val code: String) {
    RU("ru"),
    EN("en");

    companion object {
        /** Язык по умолчанию, если локаль не распознана (аудитория RuStore — русский). */
        val DEFAULT = RU

        /** Язык по коду (`ru`, `en`); неизвестный/пустой → [DEFAULT]. */
        fun fromCode(code: String?): Lang =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DEFAULT

        /** Язык системы — для первого запуска без сохранённого выбора. */
        fun system(): Lang = fromCode(Locale.getDefault().language)
    }
}
