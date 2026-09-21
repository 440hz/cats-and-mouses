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

    fun draw(
        spriteBatch: SpriteBatch,
        region: TextureRegion,
        scale: Float = 1f,
        rotation: Float = 0f,
        offsetY: Float = 0f
    ) {
        if (eaten) return
        val size = SPRITE_SIZE * scale
        val half = size / 2f
        spriteBatch.draw(
            region,
            position.x - half,
            position.y - half + offsetY,
            half,
            half,
            size,
            size,
            1f,
            1f,
            rotation
        )
    }

    companion object {
        const val SPRITE_SIZE = 48f
    }
}
