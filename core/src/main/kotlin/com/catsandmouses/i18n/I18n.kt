package com.catsandmouses.i18n

import java.util.Locale

/**
 * Локализация интерфейса: активный язык и словари RU/EN.
 *
 * Экраны не хардкодят текст — берут его через [t]. Активный язык меняется
 * в «Настройках» ([toggle]) и применяется из `ProgressStore` при старте игры.
 * Ненайденный ключ откатывается на русский, затем на имя ключа (видно в UI как ошибка).
 */
object I18n {

    /** Текущий язык интерфейса. По умолчанию — [Lang.DEFAULT] (RU). */
    var lang: Lang = Lang.DEFAULT

    /** Переключить язык на противоположный (переключатель в «Настройках»). */
    fun toggle(): Lang {
        lang = if (lang == Lang.RU) Lang.EN else Lang.RU
        return lang
    }

    /**
     * Строка по ключу. Если заданы [args], шаблон форматируется (напр. `Сыр: %1$d/%2$d`).
     * Формат — через [Locale.ROOT] (цифры и разделители не зависят от локали устройства).
     */
    fun t(key: Str, vararg args: Any): String {
        val template = table[lang]?.get(key) ?: ruTable[key] ?: key.name
        return if (args.isEmpty()) template else String.format(Locale.ROOT, template, *args)
    }

    private val ruTable: Map<Str, String> = mapOf(
        Str.TITLE_GAME to "Кошки-мышки",
        Str.TITLE_MENU to "Меню",
        Str.TITLE_GAME_OVER to "Мышка попалась!",
        Str.TITLE_WIN to "Уровень пройден!",
        Str.TITLE_FINAL to "Финал пройден!",
        Str.CELEBRATION to "УРА! ФИНАЛ ПРОЙДЕН!",
        Str.TITLE_EXIT to "Выходим?",
        Str.TITLE_SETTINGS to "Настройки",

        Str.BTN_START to "Начать",
        Str.BTN_CONTINUE to "Продолжить",
        Str.BTN_QUIT to "Выход",
        Str.BTN_RETRY to "Попробуй ещё!",
        Str.BTN_LEAVE to "Выйти",
        Str.BTN_CANCEL to "Отмена",
        Str.BTN_NEXT_LEVEL to "Следующая карта",
        Str.BTN_BACK_TO_MENU to "В меню",
        Str.BTN_PAUSE_CONTINUE to "Продолжить",
        Str.BTN_BACK to "Назад",
        Str.BTN_START_OVER to "Начать сначала",

        Str.LABEL_MUSIC to "Музыка",
        Str.LABEL_SFX to "Звуки",
        Str.LABEL_LANGUAGE to "Язык",
        Str.LANG_RU to "Рус",
        Str.LANG_EN to "Eng",

        Str.LOADING_HINT to "Нажми, чтобы начать",
        Str.HUD_CHEESE to "Сыр: %1\$d/%2\$d",
        Str.HUD_MAP to "Карта %1\$d/%2\$d",

        Str.CAT_CHASER to "Барсик",
        Str.CAT_PATROL to "Матроскин",
        Str.CAT_RANDOM to "Леопольд",
        Str.CAT_AMBUSH to "Черныш"
    )

    private val enTable: Map<Str, String> = mapOf(
        Str.TITLE_GAME to "Cats and Mouses",
        Str.TITLE_MENU to "Menu",
        Str.TITLE_GAME_OVER to "The Mouse Got Caught!",
        Str.TITLE_WIN to "Level Complete!",
        Str.TITLE_FINAL to "Final Level Cleared!",
        Str.CELEBRATION to "HOORAY! FINALE CLEARED!",
        Str.TITLE_EXIT to "Quit Game?",
        Str.TITLE_SETTINGS to "Settings",

        Str.BTN_START to "Start",
        Str.BTN_CONTINUE to "Continue",
        Str.BTN_QUIT to "Quit",
        Str.BTN_RETRY to "Try Again!",
        Str.BTN_LEAVE to "Quit",
        Str.BTN_CANCEL to "Cancel",
        Str.BTN_NEXT_LEVEL to "Next Level",
        Str.BTN_BACK_TO_MENU to "Main Menu",
        Str.BTN_PAUSE_CONTINUE to "Resume",
        Str.BTN_BACK to "Back",
        Str.BTN_START_OVER to "Start Over",

        Str.LABEL_MUSIC to "Music",
        Str.LABEL_SFX to "Sound",
        Str.LABEL_LANGUAGE to "Language",
        Str.LANG_RU to "Рус",
        Str.LANG_EN to "Eng",

        Str.LOADING_HINT to "Tap to Start",
        Str.HUD_CHEESE to "Cheese: %1\$d/%2\$d",
        Str.HUD_MAP to "Level %1\$d/%2\$d",

        Str.CAT_CHASER to "Hunter",
        Str.CAT_PATROL to "Prowler",
        Str.CAT_RANDOM to "Ziggy",
        Str.CAT_AMBUSH to "Shadow"
    )

    private val table: Map<Lang, Map<Str, String>> = mapOf(
        Lang.RU to ruTable,
        Lang.EN to enTable
    )
}
