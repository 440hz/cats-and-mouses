package com.catsandmouses.entities

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.catsandmouses.Direction
import com.catsandmouses.MazeLoader

sealed class CatStrategy {
    object Chaser : CatStrategy()
    object Patrol : CatStrategy()
    object Random : CatStrategy()
}

class Cat(
    private val maze: MazeLoader,
    startCol: Int,
    startRow: Int,
    val strategy: CatStrategy,
    val color: Color,
    val name: String
) {

    val position = Vector2()
    val cell = GridPoint2(startCol, startRow)
    var direction = Direction.NONE
        private set
    private var targetCell = GridPoint2(startCol, startRow)
    private var isMoving = false
    private var timeSinceLastMove = 0f
    private val speed = 120f

    init {
        position.set(maze.cellToWorld(startCol, startRow))
    }

    fun update(delta: Float, mouseCell: GridPoint2, reservedCells: MutableSet<Pair<Int, Int>>) {
        if (!isMoving) {
            tryStartMoving(mouseCell, reservedCells)
            if (isMoving) {
                timeSinceLastMove = 0f
                reservedCells.add(targetCell.x to targetCell.y)
            } else {
                timeSinceLastMove += delta
            }
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
            timeSinceLastMove = 0f

            val chosen = chooseDirection(mouseCell, reservedCells)
            if (chosen != Direction.NONE && maze.isPassable(cell.x + chosen.deltaCol, cell.y + chosen.deltaRow)) {
                direction = chosen
                targetCell.set(cell.x + chosen.deltaCol, cell.y + chosen.deltaRow)
                isMoving = true
                reservedCells.add(targetCell.x to targetCell.y)
            } else {
                direction = Direction.NONE
                isMoving = false
            }
        } else {
            val ratio = moveDist / dist
            position.x += diffX * ratio
            position.y += diffY * ratio
            timeSinceLastMove = 0f
        }
    }

    private fun chooseDirection(mouseCell: GridPoint2, reservedCells: MutableSet<Pair<Int, Int>>): Direction {
        val allOptions = mutableListOf<Direction>()
        val freeOptions = mutableListOf<Direction>()

        for (dir in listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)) {
            if (dir == direction.opposite()) continue
            val nextCol = cell.x + dir.deltaCol
            val nextRow = cell.y + dir.deltaRow
            if (maze.isPassable(nextCol, nextRow)) {
                allOptions.add(dir)
                if (!reservedCells.contains(nextCol to nextRow)) {
                    freeOptions.add(dir)
                }
            }
        }

        val options = when {
            freeOptions.isNotEmpty() -> freeOptions
            timeSinceLastMove > 1f -> {
                Gdx.app.log("Cat", "Cat forced move after $timeSinceLastMove seconds")
                allOptions
            }
            else -> emptyList()
        }

        if (options.isEmpty()) {
            // Тупик: разрешаем разворот
            val reverse = direction.opposite()
            if (reverse != Direction.NONE && maze.isPassable(cell.x + reverse.deltaCol, cell.y + reverse.deltaRow)) {
                return reverse
            }
            return Direction.NONE
        }

        return when (strategy) {
            is CatStrategy.Chaser -> chooseChaserDirection(options, mouseCell)
            is CatStrategy.Patrol -> choosePatrolDirection(options, mouseCell)
            is CatStrategy.Random -> chooseRandomDirection(options, mouseCell)
        }
    }

    private fun chooseChaserDirection(options: List<Direction>, mouseCell: GridPoint2): Direction {
        return bfsDirection(mouseCell, options) ?: options.random()
    }

    private fun choosePatrolDirection(options: List<Direction>, mouseCell: GridPoint2): Direction {
        val distance = bfsDistance(cell, mouseCell)
        if (distance != null && distance <= 5) {
            return bfsDirection(mouseCell, options) ?: options.random()
        }
        // Patrol: prefer directions along the perimeter (keep close to walls)
        val perimeterScores = options.map { dir ->
            val nextCol = cell.x + dir.deltaCol
            val nextRow = cell.y + dir.deltaRow
            val wallCount = Direction.values().count { d ->
                d != Direction.NONE && maze.isWall(nextCol + d.deltaCol, nextRow + d.deltaRow)
            }
            dir to wallCount
        }
        val maxWalls = perimeterScores.maxOfOrNull { it.second } ?: 0
        val best = perimeterScores.filter { it.second == maxWalls }.map { it.first }
        return best.random()
    }

    private fun chooseRandomDirection(options: List<Direction>, mouseCell: GridPoint2): Direction {
        return if (MathUtils.random() < 0.3f) {
            bfsDirection(mouseCell, options) ?: options.random()
        } else {
            options.random()
        }
    }

    private fun bfsDirection(target: GridPoint2, options: List<Direction>): Direction? {
        val path = findPath(cell, target) ?: return null
        if (path.isEmpty()) return null
        val next = path.first()
        val deltaCol = next.x - cell.x
        val deltaRow = next.y - cell.y
        return Direction.values().find { it.deltaCol == deltaCol && it.deltaRow == deltaRow && options.contains(it) }
    }

    private fun bfsDistance(start: GridPoint2, target: GridPoint2): Int? {
        val path = findPath(start, target) ?: return null
        return path.size
    }

    private fun findPath(start: GridPoint2, target: GridPoint2): List<GridPoint2>? {
        if (start == target) return emptyList()
        val queue = ArrayDeque<GridPoint2>()
        val cameFrom = mutableMapOf<GridPoint2, GridPoint2>()
        queue.add(start)
        cameFrom[start] = start

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == target) break

            for (dir in listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)) {
                val next = GridPoint2(current.x + dir.deltaCol, current.y + dir.deltaRow)
                if (maze.isPassable(next.x, next.y) && !cameFrom.containsKey(next)) {
                    queue.add(next)
                    cameFrom[next] = current
                }
            }
        }

        if (!cameFrom.containsKey(target)) return null

        val path = mutableListOf<GridPoint2>()
        var current = target
        while (current != start) {
            path.add(0, current)
            current = cameFrom[current]!!
        }
        return path
    }

    private fun tryStartMoving(mouseCell: GridPoint2, reservedCells: MutableSet<Pair<Int, Int>>) {
        val dir = chooseDirection(mouseCell, reservedCells)
        if (dir != Direction.NONE && maze.isPassable(cell.x + dir.deltaCol, cell.y + dir.deltaRow)) {
            direction = dir
            targetCell.set(cell.x + dir.deltaCol, cell.y + dir.deltaRow)
            isMoving = true
        }
    }

    fun draw(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = color
        shapeRenderer.circle(position.x, position.y, RADIUS)
    }

    companion object {
        const val RADIUS = 18f
    }
}
