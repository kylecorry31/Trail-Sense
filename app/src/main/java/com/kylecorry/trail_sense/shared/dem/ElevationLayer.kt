package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.roundNearest
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.ParallelCoroutineRunner
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ElevationLayer : ILayer {

    private val runner = CoroutineQueueRunner(2)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var lastBounds: CoordinateBounds = CoordinateBounds.empty
    private var lastMetersPerPixel: Float? = null
    private var contourCalculationInProgress = false

    private val validIntervals = listOf(
        10f,
        20f,
        40f,
        50f,
        100f
    )

    private var contours = listOf<Pair<Float, List<Pair<Coordinate, Coordinate>>>>()

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        if (SafeMode.isEnabled() || map.metersPerPixel > 75f) {
            return
        }

        val bounds = map.mapBounds
        val metersPerPixel = map.metersPerPixel
        if (!contourCalculationInProgress && (lastMetersPerPixel != map.metersPerPixel || !areBoundsEqual(
                lastBounds,
                map.mapBounds
            ))
        ) {
            scope.launch {
                // TODO: Debounce loader
                runner.enqueue {
                    contourCalculationInProgress = true
                    val interval = validIntervals.minBy {
                        // TODO: Convert to feet if needed
                        abs(it - (metersPerPixel * 2))
                    }
                    contours = getContourLines(bounds, interval)
                    lastMetersPerPixel = metersPerPixel
                    lastBounds = bounds
                    contourCalculationInProgress = false
                }
            }
        }

        drawer.stroke(AppColor.Brown.color)
        drawer.strokeWeight(drawer.dp(1f))
        drawer.opacity(127)
        drawer.noFill()
        drawer.lines(contours.flatMap { it.second }.map { line ->
            val pixel1 = map.toPixel(line.first)
            val pixel2 = map.toPixel(line.second)
            listOf(pixel1.x, pixel1.y, pixel2.x, pixel2.y)
        }.flatten().toFloatArray())
        // TODO: Labels
        drawer.opacity(255)
    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    private fun areBoundsEqual(bounds1: CoordinateBounds, bound2: CoordinateBounds): Boolean {
        return bounds1.north == bound2.north &&
                bounds1.south == bound2.south &&
                bounds1.east == bound2.east &&
                bounds1.west == bound2.west
    }

    /**
     * Get contour lines using marching squares
     */
    private suspend fun getContourLines(
        bounds: CoordinateBounds,
        interval: Float
    ): List<Pair<Float, List<Pair<Coordinate, Coordinate>>>> = onDefault {
        val grid = mutableListOf<List<Pair<Coordinate, Float>>>()
        val latInterval = (bounds.north - bounds.south) / 8
        val lonInterval = (bounds.east - bounds.west) / 8
        var lat = bounds.south - latInterval
        while (lat <= bounds.north + latInterval) {
            var row = mutableListOf<Coordinate>()
            var lon = bounds.west - lonInterval
            while (lon <= bounds.east + lonInterval) {
                row.add(Coordinate(lat, lon))
                lon += lonInterval
            }
            grid.add(DEM.getElevations(row).map { it.first to it.second.meters().distance })
            lat += latInterval
        }

        val squares = mutableListOf<List<Pair<Coordinate, Float>>>()
        for (i in 0 until grid.size - 1) {
            for (j in 0 until grid[i].size - 1) {
                val square = listOf(
                    grid[i][j],
                    grid[i][j + 1],
                    grid[i + 1][j + 1],
                    grid[i + 1][j]
                )
                squares.add(square)
            }
        }

        val minElevation = squares.minOfOrNull { it.minOf { it.second } } ?: 0f
        val maxElevation = squares.maxOfOrNull { it.maxOf { it.second } } ?: 0f

        var startElevationInterval = minElevation.roundNearest(interval)
        if (startElevationInterval < minElevation) {
            startElevationInterval += interval
        }

        val thresholds = generateSequence(startElevationInterval) { it + interval }
            .takeWhile { it <= maxElevation }
            .toList()

        thresholds.map { threshold ->
            val parallel = ParallelCoroutineRunner(8)
            threshold to parallel.map(squares) {
                marchingSquares(it, threshold)
            }.flatten()
        }
    }

    private fun marchingSquares(
        square: List<Pair<Coordinate, Float>>,
        threshold: Float
    ): List<Pair<Coordinate, Coordinate>> {
        val contourLines = mutableListOf<Pair<Coordinate, Coordinate>>()


        /**
         *
         *   A--AB--B
         *   |      |
         *  AC     BD
         *   |      |
         *   C--CD--D
         */

        val a = square[0]
        val b = square[1]
        val c = square[3]
        val d = square[2]

        val ab = getInterpolatedCoordinate(threshold, a, b)
        val ac = getInterpolatedCoordinate(threshold, a, c)
        val bd = getInterpolatedCoordinate(threshold, b, d)
        val cd = getInterpolatedCoordinate(threshold, c, d)

        // If there are exactly 2 intersections, then there is 1 line
        val intersections = listOfNotNull(ab, ac, bd, cd)
        if (intersections.size == 2) {
            contourLines.add(intersections[0] to intersections[1])
        } else if (intersections.size == 4 && a.second >= threshold) {
            contourLines.add(intersections[0] to intersections[2])
            contourLines.add(intersections[1] to intersections[3])
        } else if (intersections.size == 4 && a.second < threshold) {
            contourLines.add(intersections[0] to intersections[1])
            contourLines.add(intersections[2] to intersections[3])
        }
        return contourLines
    }

    private fun getInterpolatedCoordinate(
        value: Float,
        a: Pair<Coordinate, Float>,
        b: Pair<Coordinate, Float>
    ): Coordinate? {
        val aAbove = a.second >= value
        val bAbove = b.second >= value

        if (aAbove == bAbove) {
            return null
        }

        var pct = SolMath.norm(value, min(a.second, b.second), max(a.second, b.second))
        if (a.second > b.second) {
            pct = 1 - pct
        }
        val distance = a.first.distanceTo(b.first)
        val bearing = a.first.bearingTo(b.first)
        return a.first.plus(distance * pct.toDouble(), bearing)
    }
}