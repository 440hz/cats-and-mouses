package com.catsandmouses.entities

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.Vector2
import com.catsandmouses.Direction
import com.catsandmouses.MazeLoader

class Mouse(private val maze: MazeLoader, startCol: Int, startRow: Int) {

    val position = Vector2()
    val cell = GridPoint2(startCol, startRow)
    var direction = Direction.NONE
        private set
    private var nextDirection = Direction.NONE
    private var targetCell = GridPoint2(startCol, startRow)
    private var isMoving = false
    private val speed = 150f

    init {
        position.set(maze.cellToWorld(startCol, startRow))
    }

    fun setNextDirection(dir: Direction) {
        if (dir != Direction.NONE) {
            nextDirection = dir
        }
    }

    fun update(delta: Float) {
        if (!isMoving) {
            tryStartMoving()
            return
        }

        val target = maze.cellToWorld(targetCell.x, targetCell.y)
        val diffX = target.x - position.x
        val diffY = target.y - position.y
        val dist = kotlin.math.hypot(diffX.toDouble(), diffY.toDouble()).toFloat()
        val moveDist = speed * delta

        if (moveDist >= dist) {
            position.set(target)
            cell.set(targetCell)

            val chosen = chooseDirection()
            if (chosen != Direction.NONE && maze.isPassable(cell.x + chosen.deltaCol, cell.y + chosen.deltaRow)) {
                direction = chosen
                targetCell.set(cell.x + chosen.deltaCol, cell.y + chosen.deltaRow)
                isMoving = true
            } else {
                direction = Direction.NONE
                isMoving = false
            }
        } else {
            val ratio = moveDist / dist
            position.x += diffX * ratio
            position.y += diffY * ratio
        }
    }

    private fun chooseDirection(): Direction {
        if (nextDirection != Direction.NONE) {
            if (maze.isPassable(cell.x + nextDirection.deltaCol, cell.y + nextDirection.deltaRow)) {
                return nextDirection
            }
        }
        if (direction != Direction.NONE && maze.isPassable(cell.x + direction.deltaCol, cell.y + direction.deltaRow)) {
            return direction
        }
        return Direction.NONE
    }

    private fun tryStartMoving() {
        if (nextDirection != Direction.NONE && maze.isPassable(cell.x + nextDirection.deltaCol, cell.y + nextDirection.deltaRow)) {
            direction = nextDirection
            targetCell.set(cell.x + direction.deltaCol, cell.y + direction.deltaRow)
            isMoving = true
        }
    }

    fun draw(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = com.badlogic.gdx.graphics.Color.GRAY
        shapeRenderer.circle(position.x, position.y, RADIUS)
    }

    companion object {
        const val RADIUS = 15f
    }
}
