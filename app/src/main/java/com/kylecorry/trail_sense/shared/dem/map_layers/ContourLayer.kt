package com.kylecorry.trail_sense.shared.dem.map_layers

import android.graphics.Color
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.Contour
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMap
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMapFactory
import com.kylecorry.trail_sense.shared.dem.colors.TrailSenseVibrantElevationColorMap
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.tools.navigation.ui.MappableLocation
import com.kylecorry.trail_sense.tools.navigation.ui.MappablePath
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.map_layers.PathLayer

class ContourLayer(private val taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask()) :
    IAsyncLayer {

    private val pathLayer = PathLayer()

    init {
        taskRunner.addTask { bounds, metersPerPixel ->
            val zoomLevel = TileMath.distancePerPixelToZoom(
                metersPerPixel.toDouble(),
                (bounds.north + bounds.south) / 2
            ).coerceAtMost(maxZoomLevel)

            if (zoomLevel < minZoomLevel) {
                contours = emptyList()
                return@addTask
            }

            val interval = validIntervals[zoomLevel] ?: validIntervals.values.first()
            contours = DEM.getContourLines(bounds, interval, validResolutions[zoomLevel]!!)
            contourInterval = interval
            lastZoomLevel = zoomLevel
            var i = -10000L
            pathLayer.setPaths(contours.flatMap { level ->
                val isImportantLine = SolMath.isZero((level.elevation / contourInterval) % 5, 0.1f)
                val name = DecimalFormatter.format(
                    Distance.Companion.meters(level.elevation).convertTo(units).value, 0
                )
                val color = colorScale.getElevationColor(level.elevation)
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
    }

    private val units by lazy { AppServiceRegistry.get<UserPreferences>().baseDistanceUnits }

    private val minZoomLevel = 13
    private val maxZoomLevel = 19

    fun setPreferences(prefs: ContourMapLayerPreferences) {
        _percentOpacity = prefs.opacity.get() / 100f
        pathLayer.setShouldRenderLabels(prefs.showLabels.get())
        // TODO: More experimentation required before this is enabled for everyone
//        pathLayer.setShouldRenderSmoothPaths(isDebug())
        colorScale = ElevationColorMapFactory().getElevationColorMap(prefs.colorStrategy.get())
        invalidate()
    }

    private var colorScale: ElevationColorMap = TrailSenseVibrantElevationColorMap()

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
                13 to Distance.Companion.feet(200f).meters().value,
                14 to Distance.Companion.feet(200f).meters().value,
                15 to Distance.Companion.feet(200f).meters().value,
                16 to Distance.Companion.feet(40f).meters().value,
                17 to Distance.Companion.feet(40f).meters().value,
                18 to Distance.Companion.feet(40f).meters().value,
                19 to Distance.Companion.feet(40f).meters().value
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

        taskRunner.scheduleUpdate(map.mapBounds, map.metersPerPixel)
        pathLayer.draw(drawer, map)
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

    private var _percentOpacity: Float = 1f

    override val percentOpacity: Float
        get() = _percentOpacity

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        pathLayer.setHasUpdateListener(listener)
    }
}