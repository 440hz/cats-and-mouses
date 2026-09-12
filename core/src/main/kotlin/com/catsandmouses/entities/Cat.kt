package com.catsandmouses.entities

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
    val color: Color
) {

    val position = Vector2()
    val cell = GridPoint2(startCol, startRow)
    var direction = Direction.NONE
        private set
    private var targetCell = GridPoint2(startCol, startRow)
    private var isMoving = false
    private val speed = 120f

    init {
        position.set(maze.cellToWorld(startCol, startRow))
    }

    fun update(delta: Float, mouseCell: GridPoint2, allCats: List<Cat>) {
        if (!isMoving) {
            tryStartMoving(mouseCell, allCats)
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

            val chosen = chooseDirection(mouseCell, allCats)
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

    private fun chooseDirection(mouseCell: GridPoint2, allCats: List<Cat>): Direction {
        val options = mutableListOf<Direction>()
        for (dir in listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)) {
            if (dir == direction.opposite()) continue
            if (maze.isPassable(cell.x + dir.deltaCol, cell.y + dir.deltaRow)) {
                options.add(dir)
            }
        }

        if (options.isEmpty()) {
            // Dead end: allow reverse
            val reverse = direction.opposite()
            if (reverse != Direction.NONE && maze.isPassable(cell.x + reverse.deltaCol, cell.y + reverse.deltaRow)) {
                return reverse
            }
            return Direction.NONE
        }

        // Запрещаем направления, ведущие в клетку, занятую другим котом
        val freeOptions = options.filter { dir ->
            !isCellOccupiedByOtherCat(cell.x + dir.deltaCol, cell.y + dir.deltaRow, allCats)
        }
        val safeOptions = freeOptions.ifEmpty { options }

        return when (strategy) {
            is CatStrategy.Chaser -> chooseChaserDirection(safeOptions, mouseCell)
            is CatStrategy.Patrol -> choosePatrolDirection(safeOptions, mouseCell)
            is CatStrategy.Random -> chooseRandomDirection(safeOptions, mouseCell)
        }
    }

    private fun isCellOccupiedByOtherCat(col: Int, row: Int, allCats: List<Cat>): Boolean {
        for (other in allCats) {
            if (other === this) continue
            if (other.cell.x == col && other.cell.y == row) return true
        }
        return false
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

    private fun tryStartMoving(mouseCell: GridPoint2, allCats: List<Cat>) {
        val dir = chooseDirection(mouseCell, allCats)
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
