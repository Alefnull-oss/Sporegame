package com.example.spore.game.engine

import kotlin.math.floor

/**
 * High-performance 2D Spatial Hash Grid to eliminate collision and sensing lag.
 * Reduces O(N^2) complexity to near O(1) by dividing the world into discrete cells.
 */
class SpatialGrid<T>(
    val worldWidth: Float,
    val worldHeight: Float,
    val cellSize: Float = 300f
) {
    val cols = (worldWidth / cellSize).toInt() + 1
    val rows = (worldHeight / cellSize).toInt() + 1

    val grid: Array<ArrayList<T>> = Array(cols * rows) { ArrayList(16) }

    fun clear() {
        for (i in grid.indices) {
            grid[i].clear()
        }
    }

    fun getCellIndex(x: Float, y: Float): Int {
        var col = floor(x / cellSize).toInt() % cols
        if (col < 0) col += cols
        var row = floor(y / cellSize).toInt() % rows
        if (row < 0) row += rows
        return row * cols + col
    }

    fun insert(x: Float, y: Float, item: T) {
        val idx = getCellIndex(x, y)
        grid[idx].add(item)
    }

    /**
     * Queries all items in the 3x3 neighboring cells surrounding (x, y), wrapping seamlessly across edges.
     */
    inline fun forEachNeighbor(x: Float, y: Float, action: (T) -> Unit) {
        val centerCol = floor(x / cellSize).toInt()
        val centerRow = floor(y / cellSize).toInt()

        for (dr in -1..1) {
            val r = ((centerRow + dr) % rows + rows) % rows
            val rowOffset = r * cols
            for (dc in -1..1) {
                val c = ((centerCol + dc) % cols + cols) % cols
                val list = grid[rowOffset + c]
                for (i in 0 until list.size) {
                    action(list[i])
                }
            }
        }
    }
}
