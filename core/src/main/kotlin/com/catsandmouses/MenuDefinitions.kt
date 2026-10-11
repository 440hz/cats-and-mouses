package com.catsandmouses

import com.catsandmouses.i18n.I18n
import com.catsandmouses.i18n.Str

/**
 * Декларативные описания меню, оверлеев и их кнопок.
 * Источник истины — MENU.md, §3. Экраны строят кнопки по этим спецификациям.
 *
 * Надписи локализованы через [I18n] (RU/EN), поэтому вместо констант — функции.
 */
object MenuDefinitions {

    // Заголовки оверлеев и экранов
    fun titleGame(): String = I18n.t(Str.TITLE_GAME)
    fun titleMenu(): String = I18n.t(Str.TITLE_MENU)
    fun titleGameOver(): String = I18n.t(Str.TITLE_GAME_OVER)
    fun titleWin(): String = I18n.t(Str.TITLE_WIN)
    fun titleFinal(): String = I18n.t(Str.TITLE_FINAL)
    fun titleExit(): String = I18n.t(Str.TITLE_EXIT)
    fun titleSettings(): String = I18n.t(Str.TITLE_SETTINGS)

    // Надписи кнопок
    fun btnStart(): String = I18n.t(Str.BTN_START)
    fun btnContinue(): String = I18n.t(Str.BTN_CONTINUE)
    fun btnQuit(): String = I18n.t(Str.BTN_QUIT)
    fun btnRetry(): String = I18n.t(Str.BTN_RETRY)
    fun btnLeave(): String = I18n.t(Str.BTN_LEAVE)
    fun btnCancel(): String = I18n.t(Str.BTN_CANCEL)
    fun btnNextLevel(): String = I18n.t(Str.BTN_NEXT_LEVEL)
    fun btnBackToMenu(): String = I18n.t(Str.BTN_BACK_TO_MENU)
    fun btnPauseContinue(): String = I18n.t(Str.BTN_PAUSE_CONTINUE)
    fun btnBack(): String = I18n.t(Str.BTN_BACK)
    fun btnStartOver(): String = I18n.t(Str.BTN_START_OVER)

    // Строки настроек (ползунки + язык, MENU.md §3.7)
    fun labelMusic(): String = I18n.t(Str.LABEL_MUSIC)
    fun labelSfx(): String = I18n.t(Str.LABEL_SFX)
    fun labelLanguage(): String = I18n.t(Str.LABEL_LANGUAGE)

    /** Значение строки «Язык» — имя текущего языка (`Рус`/`Eng`). */
    fun languageValue(): String =
        I18n.t(if (I18n.lang == com.catsandmouses.i18n.Lang.RU) Str.LANG_RU else Str.LANG_EN)

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
        val specs = mutableListOf(ButtonSpec(btnStart(), Action.START_NEW))
        if (hasProgress) specs.add(ButtonSpec(btnContinue(), Action.CONTINUE))
        specs.add(ButtonSpec(btnQuit(), Action.QUIT_APP))
        return specs
    }

    /** Оверлей «Мышка попалась!» (§3.2). */
    fun gameOver(): List<ButtonSpec> = listOf(
        ButtonSpec(btnRetry(), Action.RESTART_LEVEL),
        ButtonSpec(btnLeave(), Action.TO_MENU)
    )

    /** Оверлей «Победа» (§3.3). */
    fun win(isFinal: Boolean): List<ButtonSpec> =
        if (isFinal) {
            listOf(ButtonSpec(btnBackToMenu(), Action.TO_MENU))
        } else {
            listOf(ButtonSpec(btnNextLevel(), Action.NEXT_LEVEL))
        }

    /** Оверлей «Выходим?» (§3.4). */
    fun exitConfirm(): List<ButtonSpec> = listOf(
        ButtonSpec(btnCancel(), Action.CANCEL),
        ButtonSpec(btnLeave(), Action.QUIT_APP)
    )

    /** Экран «Настройки» (§3.7): только кнопка «Назад»; ползунки задаёт SettingsScreen. */
    fun settings(): List<ButtonSpec> = listOf(
        ButtonSpec(btnBack(), Action.BACK)
    )
}
