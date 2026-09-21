package com.catsandmouses.entities

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
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
    private var rotation = 0f

    init {
        position.set(maze.cellToWorld(startCol, startRow))
    }

    fun setNextDirection(dir: Direction) {
        if (dir != Direction.NONE) {
            nextDirection = dir
        }
    }

    /** Мгновенно перенести мышь в клетку (телепорт после поимки). Сбрасывает движение. */
    fun teleportTo(col: Int, row: Int) {
        position.set(maze.cellToWorld(col, row))
        cell.set(col, row)
        targetCell.set(col, row)
        direction = Direction.NONE
        nextDirection = Direction.NONE
        isMoving = false
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
                rotation = rotationFor(chosen)
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
            rotation = rotationFor(nextDirection)
            targetCell.set(cell.x + direction.deltaCol, cell.y + direction.deltaRow)
            isMoving = true
        }
    }

    fun draw(spriteBatch: SpriteBatch, region: TextureRegion) {
        val size = SPRITE_SIZE
        val half = size / 2f
        // Спрайт мыши теперь нарисован «вниз», как у котов;
        // компенсируем разницу, чтобы мышь смотрела в направлении движения.
        val renderRotation = rotation + SPRITE_ROTATION_OFFSET
        spriteBatch.draw(
            region,
            position.x - half,
            position.y - half,
            half,
            half,
            size,
            size,
            1f,
            1f,
            renderRotation
        )
    }

    companion object {
        const val RADIUS = 15f
        const val SPRITE_SIZE = 48f
        const val SPRITE_ROTATION_OFFSET = 90f

        private fun rotationFor(dir: Direction): Float = when (dir) {
            Direction.RIGHT -> 0f
            Direction.UP -> 90f
            Direction.LEFT -> 180f
            Direction.DOWN -> 270f
            Direction.NONE -> 0f
        }
    }
}
