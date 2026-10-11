package com.catsandmouses.i18n

/**
 * Ключи локализованных строк интерфейса. Типобезопасная замена «магическим» строкам:
 * экраны не хардкодят текст, а берут его через [I18n.t].
 *
 * Источник истины для состава экранов и переходов — `MENU.md`.
 */
enum class Str {
    // Заголовки экранов и оверлеев
    TITLE_GAME,
    TITLE_MENU,
    TITLE_GAME_OVER,
    TITLE_WIN,
    TITLE_FINAL,
    TITLE_EXIT,
    TITLE_SETTINGS,

    // Надписи кнопок
    BTN_START,
    BTN_CONTINUE,
    BTN_QUIT,
    BTN_RETRY,
    BTN_LEAVE,
    BTN_CANCEL,
    BTN_NEXT_LEVEL,
    BTN_BACK_TO_MENU,
    BTN_PAUSE_CONTINUE,
    BTN_BACK,

    // Настройки (ползунки, переключатель языка — MENU.md §3.7)
    LABEL_MUSIC,
    LABEL_SFX,
    LABEL_LANGUAGE,
    LANG_RU,
    LANG_EN,

    // Заставка и HUD
    LOADING_HINT,
    HUD_CHEESE,
    HUD_MAP,

    // Имена котов (по ролям: Chaser / Patrol / Random / Ambush)
    CAT_CHASER,
    CAT_PATROL,
    CAT_RANDOM,
    CAT_AMBUSH
}
