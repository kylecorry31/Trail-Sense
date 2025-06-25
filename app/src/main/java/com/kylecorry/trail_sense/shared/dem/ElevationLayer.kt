package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.onDefault
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.ParallelCoroutineRunner
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.getConnectedLines
import com.kylecorry.trail_sense.shared.andromeda_temp.getIsolineCalculators
import com.kylecorry.trail_sense.shared.andromeda_temp.getMultiplesBetween
import com.kylecorry.trail_sense.shared.canvas.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.scales.ContinuousColorScale
import com.kylecorry.trail_sense.tools.maps.infrastructure.tiles.TileMath
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView

class ElevationLayer : ILayer {

    private val units by lazy { AppServiceRegistry.get<UserPreferences>().baseDistanceUnits }

    private val minZoomLevel = 13
    private val maxZoomLevel = 19

    // TODO: Get a better scale
    var shouldColorContours = false
    private val colorScale = ContinuousColorScale(AppColor.Green.color, AppColor.Red.color)
    private val minScaleElevation = 0f
    private val maxScaleElevation = 2000f

    private val taskRunner = MapLayerBackgroundTask()

    private val validIntervals by lazy {
        if (units.isMetric) {
            mapOf(
                13 to 50f,
                14 to 50f,
                15 to 20f,
                16 to 10f,
                17 to 50f,
                18 to 5f,
                19 to 2f
            )
        } else {
            mapOf(
                13 to Distance.feet(200f).meters().distance,
                14 to Distance.feet(200f).meters().distance,
                15 to Distance.feet(100f).meters().distance,
                16 to Distance.feet(40f).meters().distance,
                17 to Distance.feet(20f).meters().distance,
                18 to Distance.feet(20f).meters().distance,
                19 to Distance.feet(10f).meters().distance
            )
        }
    }

    private val baseResolution = 1 / 240.0
    private val validResolutions = mapOf(
        13 to baseResolution * 2,
        14 to baseResolution * 2,
        15 to baseResolution,
        16 to baseResolution / 2,
        17 to baseResolution / 2,
        18 to baseResolution / 4,
        19 to baseResolution / 4
    )

    private var contours = listOf<Pair<Float, List<List<Coordinate>>>>()

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        if (SafeMode.isEnabled() || map.metersPerPixel > 75f) {
            return
        }

        val bounds = map.mapBounds
        val metersPerPixel = map.metersPerPixel
        taskRunner.scheduleUpdate(bounds, metersPerPixel) {
            val zoomLevel = TileMath.distancePerPixelToZoom(
                metersPerPixel.toDouble(),
                (bounds.north + bounds.south) / 2
            ).coerceAtMost(maxZoomLevel)

            if (zoomLevel < minZoomLevel) {
                contours = emptyList()
                return@scheduleUpdate
            }

            val interval = validIntervals[zoomLevel] ?: validIntervals.values.first()
            contours = getContourLines(bounds, interval, zoomLevel)
        }

        drawer.strokeWeight(drawer.dp(1f))
        drawer.stroke(AppColor.Brown.color)
        drawer.opacity(127)
        drawer.noFill()
        // TODO: Draw as curve
        contours.forEach { level ->
            if (shouldColorContours) {
                drawer.stroke(
                    colorScale.getColor(
                        SolMath.norm(
                            level.first,
                            minScaleElevation,
                            maxScaleElevation,
                            true
                        )
                    )
                )
            }
            level.second.forEach { line ->
                val points = mutableListOf<Float>()
                for (i in 0 until (line.size - 1)) {
                    val pixel1 = map.toPixel(line[i])
                    val pixel2 = map.toPixel(line[i + 1])
                    points.add(pixel1.x)
                    points.add(pixel1.y)
                    points.add(pixel2.x)
                    points.add(pixel2.y)
                }
                drawer.lines(points.toFloatArray())
            }
        }

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
        interval: Float,
        zoomLevel: Int
    ): List<Pair<Float, List<List<Coordinate>>>> = onDefault {
        val resolution = validResolutions[zoomLevel]!!

        val latitudes = Interpolation.getMultiplesBetween(
            bounds.south - resolution,
            bounds.north + resolution,
            resolution
        )

        val longitudes = Interpolation.getMultiplesBetween(
            bounds.west - resolution,
            bounds.east + resolution,
            resolution
        )

        val toLookup = latitudes.map { lat ->
            longitudes.map { lon -> Coordinate(lat, lon) }
        }

        val allElevations =
            DEM.getElevations(toLookup.flatten()).map { it.first to it.second.meters().distance }
        val grid = toLookup.map {
            it.map { coord ->
                val elevation = allElevations.find { it.first == coord }?.second ?: 0f
                coord to elevation
            }
        }

        val minElevation = grid.minOfOrNull { it.minOf { it.second } } ?: 0f
        val maxElevation = grid.maxOfOrNull { it.maxOf { it.second } } ?: 0f

        val thresholds = Interpolation.getMultiplesBetween(
            minElevation,
            maxElevation,
            interval
        )

        val parallelThresholds = ParallelCoroutineRunner(16)
        parallelThresholds.map(thresholds) { threshold ->
            val calculators = Interpolation.getIsolineCalculators<Coordinate>(
                grid,
                threshold,
                ::lerpCoordinate
            )

            val parallel = ParallelCoroutineRunner(16)
            threshold to Geometry.getConnectedLines(parallel.mapFunctions(calculators).flatten())
        }
    }

    private fun lerpCoordinate(percent: Float, a: Coordinate, b: Coordinate): Coordinate {
        val distance = a.distanceTo(b)
        val bearing = a.bearingTo(b)
        return a.plus(distance * percent.toDouble(), bearing)
    }
}