package com.catsandmouses

import com.badlogic.gdx.Game

class MainGame : Game() {

    override fun create() {
        setScreen(GameScreen())
    }
}
