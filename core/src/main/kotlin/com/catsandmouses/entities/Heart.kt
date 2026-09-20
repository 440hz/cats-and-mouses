package com.catsandmouses.entities

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.Vector2
import com.catsandmouses.MazeLoader

/**
 * Сердечко на карте (символ `H`). Подбирается мышью (флаг [collected]);
 * игровой эффект пока не задан — этап 15.3.
 */
class Heart(val cell: GridPoint2, private val maze: MazeLoader) {

    val position: Vector2 = maze.cellToWorld(cell.x, cell.y)
    var collected = false
        private set

    fun collect() {
        collected = true
    }

    fun draw(spriteBatch: SpriteBatch, region: TextureRegion) {
        if (collected) return
        val half = SPRITE_SIZE / 2f
        spriteBatch.draw(region, position.x - half, position.y - half, SPRITE_SIZE, SPRITE_SIZE)
    }

    companion object {
        const val SPRITE_SIZE = 48f
    }
}
