package com.catsandmouses

object Constants {
    const val WORLD_WIDTH = 1280f
    const val WORLD_HEIGHT = 720f
    const val CELL_SIZE = 40f
    const val MAZE_COLS = 28
    const val MAZE_ROWS = 16
    const val MAZE_WIDTH = MAZE_COLS * CELL_SIZE
    const val MAZE_HEIGHT = MAZE_ROWS * CELL_SIZE
    const val MAZE_OFFSET_X = (WORLD_WIDTH - MAZE_WIDTH) / 2f
    const val MAZE_OFFSET_Y = (WORLD_HEIGHT - MAZE_HEIGHT) / 2f
    const val VERSION = "1.0.15"
}
