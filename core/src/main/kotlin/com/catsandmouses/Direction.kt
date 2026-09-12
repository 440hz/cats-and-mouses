package com.catsandmouses

enum class Direction(val deltaCol: Int, val deltaRow: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    NONE(0, 0);

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
        NONE -> NONE
    }

    companion object {
        fun fromKeys(up: Boolean, down: Boolean, left: Boolean, right: Boolean): Direction {
            return when {
                up -> UP
                down -> DOWN
                left -> LEFT
                right -> RIGHT
                else -> NONE
            }
        }
    }
}
