package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.ui.colormaps.RgbInterpolationColorMap
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.withLayerOpacity
import com.kylecorry.trail_sense.shared.canvas.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.TileMath
import com.kylecorry.trail_sense.tools.navigation.ui.layers.ILayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView

class ElevationLayer : ILayer {

    private val units by lazy { AppServiceRegistry.get<UserPreferences>().baseDistanceUnits }

    private val minZoomLevel = 13
    private val maxZoomLevel = 19

    var shouldColorContours = false
    private val colorScale = RgbInterpolationColorMap(
        arrayOf(
            0xFF006400.toInt(), // Dark green (0m)
            0xFF90EE90.toInt(), // Light green (~500m)
            0xFFFFFF00.toInt(), // Yellow (~1000m)
            0xFFA52A2A.toInt(), // Brown (~1500m)
            0xFFFF4500.toInt(), // Orange (~2000m)
            0xFF800080.toInt()  // Purple (~3000m)
        )
    )
    private val minScaleElevation = 0f
    private val maxScaleElevation = 3000f

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
                19 to 5f
            )
        } else {
            mapOf(
                13 to Distance.feet(200f).meters().distance,
                14 to Distance.feet(200f).meters().distance,
                15 to Distance.feet(100f).meters().distance,
                16 to Distance.feet(40f).meters().distance,
                17 to Distance.feet(20f).meters().distance,
                18 to Distance.feet(20f).meters().distance,
                19 to Distance.feet(20f).meters().distance
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
    private var contourInterval = 1f

    override fun draw(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        if (SafeMode.isEnabled() || map.metersPerPixel > 75f) {
            return
        }

        taskRunner.scheduleUpdate(map.mapBounds, map.metersPerPixel) { bounds, metersPerPixel ->
            val zoomLevel = TileMath.distancePerPixelToZoom(
                metersPerPixel.toDouble(),
                (bounds.north + bounds.south) / 2
            ).coerceAtMost(maxZoomLevel)

            if (zoomLevel < minZoomLevel) {
                contours = emptyList()
                return@scheduleUpdate
            }

            val interval = validIntervals[zoomLevel] ?: validIntervals.values.first()
            contours = DEM.getContourLines(bounds, interval, validResolutions[zoomLevel]!!)
            contourInterval = interval
        }

        drawer.stroke(AppColor.Brown.color)
        drawer.noFill()

        drawer.withLayerOpacity(127) {
            val thickLineWeight = drawer.dp(2.5f)
            val thinLineWeight = drawer.dp(1f)

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
                drawer.strokeWeight(if (SolMath.isZero((level.first / contourInterval) % 5)) thickLineWeight else thinLineWeight)
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
        }

    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        // TODO: Draw color scale
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
}