package com.kylecorry.trail_sense.shared.andromeda_temp.geopoint

import com.kylecorry.sol.math.arithmetic.Arithmetic
import kotlin.math.floor

/**
 * The square cells of a [GeoPointFile] and the positions within them. Cells are numbered in rows from the south pole,
 * and each row from the antimeridian east. A position within a cell is y * steps + x, in steps of 10^-decimals degrees.
 *
 * A position within a cell is stored as a Morton code of its x and y.
 */
class GeoPointGrid(val cellDegrees: Int, decimals: Int) {

    init {
        require(isValid(cellDegrees, decimals)) { "Unsupported grid: $cellDegrees degrees, $decimals decimals" }
    }

    val rows = 180 / cellDegrees
    val columns = 360 / cellDegrees
    val cellCount = rows * columns
    private val stepsPerDegree = Arithmetic.power(10, decimals).toLong()
    private val steps = cellDegrees * stepsPerDegree

    // The bits per axis of a position within a cell
    private val bits = 64 - java.lang.Long.numberOfLeadingZeros(steps - 1)

    /**
     * The cells that cover an area, row by row. The area can extend past the antimeridian (a west of -190 is 170).
     */
    fun getCells(south: Double, north: Double, west: Double, east: Double): List<Int> {
        val firstColumn = floor((west + 180) / cellDegrees).toInt()
        val lastColumn = floor((east + 180) / cellDegrees).toInt()
        val columnCount = (lastColumn - firstColumn + 1).coerceIn(0, columns)
        val cells = ArrayList<Int>()
        for (row in getRow(south)..getRow(north)) {
            for (i in 0 until columnCount) {
                cells.add(row * columns + Math.floorMod(firstColumn + i, columns))
            }
        }
        return cells
    }

    /**
     * The position (y * steps + x) of a Morton code of a position within a cell. The bits of x are at the even bits of
     * the code and the bits of y at the odd bits.
     */
    fun getPosition(code: Long): Long {
        require(code ushr (2 * bits) == 0L) { "A position is outside of its cell" }
        var y = 0L
        var x = 0L
        for (i in 0 until bits) {
            x = x or (((code shr (2 * i)) and 1L) shl i)
            y = y or (((code shr (2 * i + 1)) and 1L) shl i)
        }
        require(y < steps && x < steps) { "A position is outside of its cell" }
        return y * steps + x
    }

    // Subtracting whole steps before dividing gives the closest double to the stored decimal
    fun getLatitude(cell: Int, position: Long): Double {
        return ((cell / columns) * steps + position / steps - 90 * stepsPerDegree).toDouble() / stepsPerDegree
    }

    fun getLongitude(cell: Int, position: Long): Double {
        return ((cell % columns) * steps + position % steps - 180 * stepsPerDegree).toDouble() / stepsPerDegree
    }

    private fun getRow(latitude: Double): Int {
        return floor((latitude + 90) / cellDegrees).toInt().coerceIn(0, rows - 1)
    }

    companion object {
        private const val MAX_DECIMALS = 7

        private fun isValid(cellDegrees: Int, decimals: Int): Boolean {
            return cellDegrees > 0 && 180 % cellDegrees == 0 && 360 % cellDegrees == 0 && decimals in 0..MAX_DECIMALS
        }
    }
}
