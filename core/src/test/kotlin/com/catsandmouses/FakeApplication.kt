package com.catsandmouses

import com.badlogic.gdx.Application
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.ApplicationLogger
import com.badlogic.gdx.Audio
import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.Input
import com.badlogic.gdx.LifecycleListener
import com.badlogic.gdx.Net
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.utils.Clipboard

/**
 * Минимальная реализация [Application] для тестов: без окна, потока рендера и нативных библиотек.
 * Логи — no-op, тип — Desktop, остальное отдаётся из статики [Gdx].
 */
class FakeApplication : Application {

    override fun getApplicationListener(): ApplicationListener? = null

    override fun getGraphics(): Graphics? = null

    override fun getAudio(): Audio? = null

    override fun getInput(): Input? = Gdx.input

    override fun getFiles(): Files? = Gdx.files

    override fun getNet(): Net? = null

    override fun log(tag: String?, message: String?) {}

    override fun log(tag: String?, message: String?, exception: Throwable?) {}

    override fun error(tag: String?, message: String?) {}

    override fun error(tag: String?, message: String?, exception: Throwable?) {}

    override fun debug(tag: String?, message: String?) {}

    override fun debug(tag: String?, message: String?, exception: Throwable?) {}

    override fun setLogLevel(logLevel: Int) {}

    override fun getLogLevel(): Int = Application.LOG_NONE

    override fun setApplicationLogger(applicationLogger: ApplicationLogger?) {}

    override fun getApplicationLogger(): ApplicationLogger? = null

    override fun getType(): Application.ApplicationType = Application.ApplicationType.Desktop

    override fun getVersion(): Int = 0

    override fun getJavaHeap(): Long = Runtime.getRuntime().totalMemory()

    override fun getNativeHeap(): Long = 0L

    override fun getPreferences(name: String?): Preferences = FakePreferences()

    override fun getClipboard(): Clipboard? = null

    override fun postRunnable(runnable: Runnable?) {
        runnable?.run()
    }

    override fun exit() {}

    override fun addLifecycleListener(listener: LifecycleListener?) {}

    override fun removeLifecycleListener(listener: LifecycleListener?) {}
}
