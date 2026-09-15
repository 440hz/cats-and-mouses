package com.catsandmouses.entities

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.Vector2
import com.catsandmouses.MazeLoader

class Pellet(val cell: GridPoint2, private val maze: MazeLoader) {

    val position: Vector2 = maze.cellToWorld(cell.x, cell.y)
    var eaten = false
        private set

    fun eat() {
        eaten = true
    }

    fun draw(spriteBatch: SpriteBatch, region: TextureRegion) {
        if (eaten) return
        val half = SPRITE_SIZE / 2f
        spriteBatch.draw(region, position.x - half, position.y - half, SPRITE_SIZE, SPRITE_SIZE)
    }

    companion object {
        const val SPRITE_SIZE = 48f
    }
}
