package com.catsandmouses

import android.os.Bundle
import android.util.Log
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
        }
        // Системная локаль: JVM-локаль на Android может не совпадать с выбранной в системе
        val systemLocale = resources.configuration.locales[0]
        Log.i("AndroidLauncher", "systemLocale=$systemLocale jvmLocale=${java.util.Locale.getDefault()}")
        initialize(MainGame(platformLangCode = systemLocale.language), config)
    }
}
