package com.catsandmouses.lwjgl3

/**
 * Мост между окном Lwjgl3 и активным экраном редактора: позволяет перехватить
 * закрытие окна (крестик) и сначала спросить подтверждение.
 *
 * Редактор устанавливает обработчик в `show()` и снимает в `hide()`/`dispose()`,
 * поэтому во время playtest и в обычной игре перехвата нет.
 */
internal object EditorWindowClose {

    /** true — закрытие окна отменено (показан диалог), false — закрывать. */
    var handler: (() -> Boolean)? = null

    fun request(): Boolean = handler?.invoke() ?: false
}
