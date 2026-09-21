package com.catsandmouses

/**
 * Декларативные описания меню, оверлеев и их кнопок.
 * Источник истины — MENU.md, §3. Экраны строят кнопки по этим спецификациям.
 */
object MenuDefinitions {

    // Заголовки оверлеев и экранов
    const val TITLE_GAME = "Кошки-мышки"
    const val TITLE_MENU = "Меню"
    const val TITLE_GAME_OVER = "Мышка попалась!"
    const val TITLE_WIN = "Уровень пройден!"
    const val TITLE_FINAL = "Финал пройден!"
    const val TITLE_EXIT = "Выходим?"
    const val TITLE_SETTINGS = "Настройки"

    // Надписи кнопок
    const val BTN_START = "Начать"
    const val BTN_CONTINUE = "Продолжить"
    const val BTN_QUIT = "Выход"
    const val BTN_RETRY = "Попробуй ещё!"
    const val BTN_LEAVE = "Выйти"
    const val BTN_CANCEL = "Отмена"
    const val BTN_NEXT_LEVEL = "Следующая карта"
    const val BTN_BACK_TO_MENU = "В меню"
    const val BTN_PAUSE_CONTINUE = "Продолжить"
    const val BTN_BACK = "Назад"

    // Строки настроек (ползунки, MENU.md §3.7)
    const val LABEL_MUSIC = "Музыка"
    const val LABEL_SFX = "Звуки"

    /** Подпись ползунка со значением, напр. «Музыка 80%». */
    fun volumeLabel(prefix: String, value: Int): String = "$prefix $value%"

    /** Действие, стоящее за кнопкой меню/оверлея. */
    enum class Action {
        START_NEW,       // «Начать» — сброс прогресса, карта 1
        CONTINUE,        // «Продолжить» — с карты гибели
        QUIT_APP,        // «Выход» — выход из приложения
        RESTART_LEVEL,   // «Попробуй ещё!» — та же карта
        NEXT_LEVEL,      // «Следующая карта»
        TO_MENU,         // в главное меню
        CANCEL,          // «Отмена» — вернуться в игру
        BACK             // «Назад» — из настроек в меню
    }

    /** Одна кнопка: надпись и действие. */
    data class ButtonSpec(val label: String, val action: Action)

    /** Главное меню (§3.1). */
    fun mainMenu(hasProgress: Boolean): List<ButtonSpec> {
        val specs = mutableListOf(ButtonSpec(BTN_START, Action.START_NEW))
        if (hasProgress) specs.add(ButtonSpec(BTN_CONTINUE, Action.CONTINUE))
        specs.add(ButtonSpec(BTN_QUIT, Action.QUIT_APP))
        return specs
    }

    /** Оверлей «Мышка попалась!» (§3.2). */
    fun gameOver(): List<ButtonSpec> = listOf(
        ButtonSpec(BTN_RETRY, Action.RESTART_LEVEL),
        ButtonSpec(BTN_LEAVE, Action.TO_MENU)
    )

    /** Оверлей «Победа» (§3.3). */
    fun win(isFinal: Boolean): List<ButtonSpec> =
        if (isFinal) {
            listOf(ButtonSpec(BTN_BACK_TO_MENU, Action.TO_MENU))
        } else {
            listOf(ButtonSpec(BTN_NEXT_LEVEL, Action.NEXT_LEVEL))
        }

    /** Оверлей «Выходим?» (§3.4). */
    fun exitConfirm(): List<ButtonSpec> = listOf(
        ButtonSpec(BTN_CANCEL, Action.CANCEL),
        ButtonSpec(BTN_LEAVE, Action.QUIT_APP)
    )

    /** Экран «Настройки» (§3.7): только кнопка «Назад»; ползунки задаёт SettingsScreen. */
    fun settings(): List<ButtonSpec> = listOf(
        ButtonSpec(BTN_BACK, Action.BACK)
    )
}
