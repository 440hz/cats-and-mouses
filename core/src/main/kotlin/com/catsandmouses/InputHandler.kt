package com.catsandmouses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

class InputHandler {

    fun getDirection(): Direction {
        val up = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)
        val down = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)
        val left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)
        val right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)
        return Direction.fromKeys(up, down, left, right)
    }
}
