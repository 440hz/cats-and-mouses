package com.catsandmouses

import com.badlogic.gdx.Game

class MainGame(
    private val startMap: String = "1/1",
    private val debugLogs: Boolean = false
) : Game() {

    override fun create() {
        setScreen(GameScreen(startMap, debugLogs))
    }
}
