package com.kylecorry.trail_sense.shared.dem

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.ui.colormaps.RgbInterpolationColorMap
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.toDegrees
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.withLayerOpacity
import com.kylecorry.trail_sense.shared.canvas.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.ContourMapLayerPreferences
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles.TileMath
import kotlin.math.absoluteValue
import kotlin.math.atan2

class ContourLayer : IAsyncLayer {

    private val units by lazy { AppServiceRegistry.get<UserPreferences>().baseDistanceUnits }
    private var updateListener: (() -> Unit)? = null

    private val minZoomLevel = 13
    private val maxZoomLevel = 19

    private var opacity = 127

    fun setPreferences(prefs: ContourMapLayerPreferences) {
        opacity = SolMath.map(
            prefs.opacity.toFloat(),
            0f,
            100f,
            0f,
            255f,
            shouldClamp = true
        ).toInt()
        shouldDrawLabels = prefs.showLabels
        shouldColorContours = prefs.colorWithElevation
        invalidate()
    }

    private var shouldDrawLabels = true
    private var shouldColorContours = false
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

    private var contours = listOf<Contour>()
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
            onMain {
                updateListener?.invoke()
            }
        }

        drawer.textSize(drawer.sp(10f * map.layerScale))
        drawer.withLayerOpacity(opacity) {
            val thickLineWeight = drawer.dp(2.5f)
            val thinLineWeight = drawer.dp(1f)

            // TODO: Draw as curve
            contours.forEach { level ->
                val isImportantLine = SolMath.isZero((level.elevation / contourInterval) % 5)

                val canvasCenter = map.toPixel(map.mapCenter)
                level.lines.forEach { line ->
                    val points = mutableListOf<Float>()
                    var closestToCenterSegment: Pair<PixelCoordinate, PixelCoordinate>? = null
                    var closestToCenterDistance: Float = Float.MAX_VALUE

                    for (i in 0 until (line.size - 1)) {
                        val pixel1 = map.toPixel(line[i])
                        val pixel2 = map.toPixel(line[i + 1])
                        points.add(pixel1.x)
                        points.add(pixel1.y)
                        points.add(pixel2.x)
                        points.add(pixel2.y)

                        val center = PixelCoordinate(
                            (pixel1.x + pixel2.x) / 2,
                            (pixel1.y + pixel2.y) / 2
                        )

                        if (center.distanceTo(canvasCenter) < closestToCenterDistance) {
                            closestToCenterDistance = center.distanceTo(canvasCenter)
                            closestToCenterSegment = Pair(pixel1, pixel2)
                        }
                    }

                    drawer.strokeWeight(if (isImportantLine) thickLineWeight else thinLineWeight)
                    drawer.noFill()

                    // TODO: Extract this
                    if (shouldColorContours) {
                        drawer.stroke(
                            colorScale.getColor(
                                SolMath.norm(
                                    level.elevation,
                                    minScaleElevation,
                                    maxScaleElevation,
                                    true
                                )
                            )
                        )
                    } else {
                        drawer.stroke(AppColor.Brown.color)
                    }
                    drawer.lines(points.toFloatArray())

                    if (isImportantLine && closestToCenterSegment != null && shouldDrawLabels) {
                        val center = closestToCenterSegment.first.midpoint(
                            closestToCenterSegment.second
                        )
                        val angle =
                            closestToCenterSegment.first.angleTo(closestToCenterSegment.second)
                        var drawAngle = if (angle.absoluteValue > 90) angle + 180 else angle
                        // TODO: Try to align it uphill
                        drawer.textMode(TextMode.Center)
                        drawer.stroke(Color.WHITE)
                        drawer.fill(Color.BLACK)
                        drawer.push()
                        drawer.rotate(
                            drawAngle,
                            center.x,
                            center.y
                        )
                        drawer.text(
                            DecimalFormatter.format(
                                Distance.meters(level.elevation).convertTo(units).distance, 0
                            ),
                            center.x,
                            center.y
                        )
                        drawer.pop()
                    }
                }
            }
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

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }

    private fun PixelCoordinate.midpoint(other: PixelCoordinate): PixelCoordinate {
        return PixelCoordinate(
            (this.x + other.x) / 2,
            (this.y + other.y) / 2
        )
    }

    private fun PixelCoordinate.angleTo(other: PixelCoordinate): Float {
        return atan2(
            other.y - this.y,
            other.x - this.x
        ).toDegrees()
    }
}