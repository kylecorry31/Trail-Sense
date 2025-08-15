package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.ui.colormaps.RgbInterpolationColorMap
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.withLayerOpacity
import com.kylecorry.trail_sense.shared.canvas.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.dem.Contour
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.tools.navigation.ui.MappableLocation
import com.kylecorry.trail_sense.tools.navigation.ui.MappablePath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath

class ContourLayer : IAsyncLayer {

    private val pathLayer = PathLayer()

    private val units by lazy { AppServiceRegistry.get<UserPreferences>().baseDistanceUnits }

    private val minZoomLevel = 13
    private val maxZoomLevel = 19

    private var opacity = 127

    fun setPreferences(prefs: ContourMapLayerPreferences) {
        opacity = SolMath.map(
            prefs.opacity.get().toFloat(),
            0f,
            100f,
            0f,
            255f,
            shouldClamp = true
        ).toInt()
        pathLayer.setShouldRenderLabels(prefs.showLabels.get())
        // TODO: More experimentation required before this is enabled for everyone
//        pathLayer.setShouldRenderSmoothPaths(isDebug())
        shouldColorContours = prefs.colorWithElevation.get()
        invalidate()
    }

    private var shouldColorContours = false
    private val colorScale = RgbInterpolationColorMap(
        arrayOf(
            0xFF006400.toInt(), // Dark green (0m)
            0xFF90EE90.toInt(), // Light green (~500m)
            0xFFFFFF00.toInt(), // Yellow (~1000m)
            0xFFA52A2A.toInt(), // Brown (~1500m)
            0xFFFF4500.toInt(), // Orange (~2000m)
            0xFF800080.toInt()  // Purple (~3000m)
            /*
            // These don't work well when the background color is green (they are meant to fill in the contours)
            // https://en.wikipedia.org/wiki/Wikipedia:WikiProject_Maps/Conventions/Topographic_maps
            0xACD0A5, // 0m
            0x94BF8B, // 250m
            0xA8C68F, // 500m
            0xBDCC96, // 750m
            0xD1D7AB, // 1000m
            0xE1E4B5, // 1250m
            0xEFEBC0, // 1500m
            0xE8E1B6, // 1750m
            0xDED6A3, // 2000m
            0xD3CA9D, // 2250m
            0xCAB982, // 2500m
            0xC3A76B, // 2750m
            0xB9985A, // 3000m
            0xAA8753, // 3250m
            0xAC9A7C, // 3500m
            0xBAAE9A, // 3750m
            0xCAC3B8, // 4000m
            0xE0DED8, // 4250m
            0xF5F4F2  // 4500m
             */
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
                15 to 50f,
                16 to 10f,
                17 to 10f,
                18 to 10f,
                19 to 10f
            )
        } else {
            mapOf(
                13 to Distance.Companion.feet(200f).meters().distance,
                14 to Distance.Companion.feet(200f).meters().distance,
                15 to Distance.Companion.feet(200f).meters().distance,
                16 to Distance.Companion.feet(40f).meters().distance,
                17 to Distance.Companion.feet(40f).meters().distance,
                18 to Distance.Companion.feet(40f).meters().distance,
                19 to Distance.Companion.feet(40f).meters().distance
            )
        }
    }

    private val baseResolution = 1 / 240.0
    private val validResolutions = mapOf(
        13 to baseResolution,
        14 to baseResolution / 2,
        15 to baseResolution / 4,
        16 to baseResolution / 4,
        17 to baseResolution / 4,
        18 to baseResolution / 4,
        19 to baseResolution / 4
    )

    private val showLabelsOnAllContoursZoomLevels = setOf(
        14, 15, 19
    )

    private var contours = listOf<Contour>()
    private var contourInterval = 1f
    private var lastZoomLevel = -1

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
            lastZoomLevel = zoomLevel
            var i = -10000L
            pathLayer.setPaths(contours.flatMap { level ->
                val isImportantLine = SolMath.isZero((level.elevation / contourInterval) % 5, 0.1f)
                val name = DecimalFormatter.format(
                    Distance.Companion.meters(level.elevation).convertTo(units).distance, 0
                )
                val color = if (shouldColorContours) {
                    colorScale.getColor(
                        SolMath.norm(
                            level.elevation,
                            minScaleElevation,
                            maxScaleElevation,
                            true
                        )
                    )
                } else {
                    AppColor.Brown.color
                }
                level.lines.map { line ->
                    MappablePath(
                        i++,
                        line.map {
                            MappableLocation(0, it, Color.TRANSPARENT, null)
                        },
                        color,
                        LineStyle.Solid,
                        name = if (isImportantLine || showLabelsOnAllContoursZoomLevels.contains(
                                lastZoomLevel
                            )
                        ) {
                            name
                        } else {
                            null
                        },
                        thicknessScale = if (isImportantLine) {
                            0.8f
                        } else {
                            0.4f
                        }
                    )
                }

            })
        }

        drawer.withLayerOpacity(opacity) {
            pathLayer.draw(drawer, map)
        }

    }

    override fun drawOverlay(
        drawer: ICanvasDrawer,
        map: IMapView
    ) {
        pathLayer.drawOverlay(drawer, map)
    }

    override fun invalidate() {
        pathLayer.invalidate()
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        pathLayer.setHasUpdateListener(listener)
    }
}