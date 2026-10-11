package com.catsandmouses

import com.catsandmouses.MenuDefinitions.Action
import com.catsandmouses.i18n.I18n
import com.catsandmouses.i18n.Lang
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MenuDefinitionsTest {

    @BeforeEach
    fun setRussian() {
        I18n.lang = Lang.RU
    }

    @AfterEach
    fun resetLanguage() {
        I18n.lang = Lang.RU
    }

    @Test
    fun `заголовки и надписи заданы (RU)`() {
        assertEquals("Кошки-мышки", MenuDefinitions.titleGame())
        assertEquals("Меню", MenuDefinitions.titleMenu())
        assertEquals("Мышка попалась!", MenuDefinitions.titleGameOver())
        assertEquals("Уровень пройден!", MenuDefinitions.titleWin())
        assertEquals("Финал пройден!", MenuDefinitions.titleFinal())
        assertEquals("Выходим?", MenuDefinitions.titleExit())

        assertEquals("Начать", MenuDefinitions.btnStart())
        assertEquals("Продолжить", MenuDefinitions.btnContinue())
        assertEquals("Выход", MenuDefinitions.btnQuit())
        assertEquals("Попробуй ещё!", MenuDefinitions.btnRetry())
        assertEquals("Выйти", MenuDefinitions.btnLeave())
        assertEquals("Отмена", MenuDefinitions.btnCancel())
        assertEquals("Следующая карта", MenuDefinitions.btnNextLevel())
        assertEquals("В меню", MenuDefinitions.btnBackToMenu())
        assertEquals("Продолжить", MenuDefinitions.btnPauseContinue())
        assertEquals("Настройки", MenuDefinitions.titleSettings())
        assertEquals("Назад", MenuDefinitions.btnBack())
        assertEquals("Музыка", MenuDefinitions.labelMusic())
        assertEquals("Звуки", MenuDefinitions.labelSfx())
        assertEquals("Язык", MenuDefinitions.labelLanguage())
    }

    @Test
    fun `заголовки и надписи заданы (EN)`() {
        I18n.lang = Lang.EN
        assertEquals("Cats and Mouses", MenuDefinitions.titleGame())
        assertEquals("Menu", MenuDefinitions.titleMenu())
        assertEquals("The Mouse Got Caught!", MenuDefinitions.titleGameOver())
        assertEquals("Level Complete!", MenuDefinitions.titleWin())
        assertEquals("Final Level Cleared!", MenuDefinitions.titleFinal())
        assertEquals("Quit Game?", MenuDefinitions.titleExit())

        assertEquals("Start", MenuDefinitions.btnStart())
        assertEquals("Continue", MenuDefinitions.btnContinue())
        assertEquals("Quit", MenuDefinitions.btnQuit())
        assertEquals("Try Again!", MenuDefinitions.btnRetry())
        assertEquals("Cancel", MenuDefinitions.btnCancel())
        assertEquals("Next Level", MenuDefinitions.btnNextLevel())
        assertEquals("Main Menu", MenuDefinitions.btnBackToMenu())
        assertEquals("Resume", MenuDefinitions.btnPauseContinue())
        assertEquals("Settings", MenuDefinitions.titleSettings())
        assertEquals("Back", MenuDefinitions.btnBack())
        assertEquals("Music", MenuDefinitions.labelMusic())
        assertEquals("Sound", MenuDefinitions.labelSfx())
        assertEquals("Language", MenuDefinitions.labelLanguage())
    }

    @Test
    fun `значение строки языка соответствует текущему языку`() {
        I18n.lang = Lang.RU
        assertEquals("Рус", MenuDefinitions.languageValue())
        I18n.lang = Lang.EN
        assertEquals("Eng", MenuDefinitions.languageValue())
    }

    @Test
    fun `главное меню без прогресса`() {
        val specs = MenuDefinitions.mainMenu(hasProgress = false)
        assertEquals(listOf(Action.START_NEW, Action.QUIT_APP), specs.map { it.action })
        assertEquals(listOf(MenuDefinitions.btnStart(), MenuDefinitions.btnQuit()), specs.map { it.label })
    }

    @Test
    fun `главное меню с прогрессом содержит Продолжить вторым`() {
        val specs = MenuDefinitions.mainMenu(hasProgress = true)
        assertEquals(listOf(Action.START_NEW, Action.CONTINUE, Action.QUIT_APP), specs.map { it.action })
        assertEquals(
            listOf(MenuDefinitions.btnStart(), MenuDefinitions.btnContinue(), MenuDefinitions.btnQuit()),
            specs.map { it.label }
        )
    }

    @Test
    fun `оверлей проигрыша ведёт к перезапуску и выходу в меню`() {
        val specs = MenuDefinitions.gameOver()
        assertEquals(listOf(Action.RESTART_LEVEL, Action.TO_MENU), specs.map { it.action })
        assertEquals(listOf(MenuDefinitions.btnRetry(), MenuDefinitions.btnLeave()), specs.map { it.label })
    }

    @Test
    fun `оверлей победы на не-финале ведёт к следующей карте`() {
        val specs = MenuDefinitions.win(isFinal = false)
        assertEquals(listOf(Action.NEXT_LEVEL), specs.map { it.action })
        assertEquals(listOf(MenuDefinitions.btnNextLevel()), specs.map { it.label })
    }

    @Test
    fun `оверлей победы на финале возвращает в меню`() {
        val specs = MenuDefinitions.win(isFinal = true)
        assertEquals(listOf(Action.TO_MENU), specs.map { it.action })
        assertEquals(listOf(MenuDefinitions.btnBackToMenu()), specs.map { it.label })
    }

    @Test
    fun `оверлей выхода содержит отмену и выход`() {
        val specs = MenuDefinitions.exitConfirm()
        assertEquals(listOf(Action.CANCEL, Action.QUIT_APP), specs.map { it.action })
        assertEquals(listOf(MenuDefinitions.btnCancel(), MenuDefinitions.btnLeave()), specs.map { it.label })
    }

    @Test
    fun `в меню без прогресса нет действия Продолжить`() {
        val actions = MenuDefinitions.mainMenu(hasProgress = false).map { it.action }
        assertTrue(actions.none { it == Action.CONTINUE })
    }

    @Test
    fun `экран настроек содержит только Назад`() {
        val specs = MenuDefinitions.settings()
        assertEquals(listOf(Action.BACK), specs.map { it.action })
        assertEquals(listOf(MenuDefinitions.btnBack()), specs.map { it.label })
    }

    @Test
    fun `подпись ползунка содержит процент`() {
        assertEquals("Музыка 80%", MenuDefinitions.volumeLabel(MenuDefinitions.labelMusic(), 80))
        assertEquals("Звуки 0%", MenuDefinitions.volumeLabel(MenuDefinitions.labelSfx(), 0))
        assertEquals("Звуки 100%", MenuDefinitions.volumeLabel(MenuDefinitions.labelSfx(), 100))
    }
}
