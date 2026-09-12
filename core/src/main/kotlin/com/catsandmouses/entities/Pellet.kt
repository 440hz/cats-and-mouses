package com.catsandmouses.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
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

    fun draw(shapeRenderer: ShapeRenderer) {
        if (eaten) return
        shapeRenderer.color = Color.WHITE
        shapeRenderer.circle(position.x, position.y, RADIUS)
    }

    companion object {
        const val RADIUS = 6f
    }
}
