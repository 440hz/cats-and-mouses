package com.catsandmouses

import com.catsandmouses.MenuDefinitions.Action
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MenuDefinitionsTest {

    @Test
    fun `заголовки и надписи заданы`() {
        assertEquals("Кошки-мышки", MenuDefinitions.TITLE_GAME)
        assertEquals("Меню", MenuDefinitions.TITLE_MENU)
        assertEquals("Мышка попалась!", MenuDefinitions.TITLE_GAME_OVER)
        assertEquals("Уровень пройден!", MenuDefinitions.TITLE_WIN)
        assertEquals("Финал пройден!", MenuDefinitions.TITLE_FINAL)
        assertEquals("Выходим?", MenuDefinitions.TITLE_EXIT)

        assertEquals("Начать", MenuDefinitions.BTN_START)
        assertEquals("Продолжить", MenuDefinitions.BTN_CONTINUE)
        assertEquals("Выход", MenuDefinitions.BTN_QUIT)
        assertEquals("Попробуй ещё!", MenuDefinitions.BTN_RETRY)
        assertEquals("Выйти", MenuDefinitions.BTN_LEAVE)
        assertEquals("Отмена", MenuDefinitions.BTN_CANCEL)
        assertEquals("Следующая карта", MenuDefinitions.BTN_NEXT_LEVEL)
        assertEquals("В меню", MenuDefinitions.BTN_BACK_TO_MENU)
    }

    @Test
    fun `главное меню без прогресса`() {
        val specs = MenuDefinitions.mainMenu(hasProgress = false)
        assertEquals(listOf(Action.START_NEW, Action.QUIT_APP), specs.map { it.action })
        assertEquals(listOf(MenuDefinitions.BTN_START, MenuDefinitions.BTN_QUIT), specs.map { it.label })
    }

    @Test
    fun `главное меню с прогрессом содержит Продолжить вторым`() {
        val specs = MenuDefinitions.mainMenu(hasProgress = true)
        assertEquals(listOf(Action.START_NEW, Action.CONTINUE, Action.QUIT_APP), specs.map { it.action })
        assertEquals(
            listOf(MenuDefinitions.BTN_START, MenuDefinitions.BTN_CONTINUE, MenuDefinitions.BTN_QUIT),
            specs.map { it.label }
        )
    }

    @Test
    fun `оверлей проигрыша ведёт к перезапуску и выходу в меню`() {
        val specs = MenuDefinitions.gameOver()
        assertEquals(listOf(Action.RESTART_LEVEL, Action.TO_MENU), specs.map { it.action })
        assertEquals(listOf(MenuDefinitions.BTN_RETRY, MenuDefinitions.BTN_LEAVE), specs.map { it.label })
    }

    @Test
    fun `оверлей победы на не-финале ведёт к следующей карте`() {
        val specs = MenuDefinitions.win(isFinal = false)
        assertEquals(listOf(Action.NEXT_LEVEL), specs.map { it.action })
        assertEquals(listOf(MenuDefinitions.BTN_NEXT_LEVEL), specs.map { it.label })
    }

    @Test
    fun `оверлей победы на финале возвращает в меню`() {
        val specs = MenuDefinitions.win(isFinal = true)
        assertEquals(listOf(Action.TO_MENU), specs.map { it.action })
        assertEquals(listOf(MenuDefinitions.BTN_BACK_TO_MENU), specs.map { it.label })
    }

    @Test
    fun `оверлей выхода содержит отмену и выход`() {
        val specs = MenuDefinitions.exitConfirm()
        assertEquals(listOf(Action.CANCEL, Action.QUIT_APP), specs.map { it.action })
        assertEquals(listOf(MenuDefinitions.BTN_CANCEL, MenuDefinitions.BTN_LEAVE), specs.map { it.label })
    }

    @Test
    fun `в меню без прогресса нет действия Продолжить`() {
        val actions = MenuDefinitions.mainMenu(hasProgress = false).map { it.action }
        assertTrue(actions.none { it == Action.CONTINUE })
    }
}
