package com.catsandmouses.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.catsandmouses.MainGame

class Lwjgl3Launcher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Lwjgl3Application(MainGame(), Lwjgl3ApplicationConfiguration().apply {
                setTitle("CatsAndMouses")
                setWindowedMode(1280, 720)
            })
        }
    }
}
