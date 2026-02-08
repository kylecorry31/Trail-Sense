package com.kylecorry.trail_sense.shared.dem.map_layers

import android.content.Context

import android.os.Bundle
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMapFactory
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorStrategy
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getPreferences
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

class ContourGeoJsonSource : GeoJsonSource {

    private val units = AppServiceRegistry.get<UserPreferences>().baseDistanceUnits

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
                13 to Distance.feet(200f).meters().value,
                14 to Distance.feet(200f).meters().value,
                15 to Distance.feet(200f).meters().value,
                16 to Distance.feet(40f).meters().value,
                17 to Distance.feet(40f).meters().value,
                18 to Distance.feet(40f).meters().value,
                19 to Distance.feet(40f).meters().value
            )
        }
    }

    private val minZoom = 13
    private val maxZoom = 19

    private val showLabelsOnAllContoursZoomLevels = setOf(
        14, 15, 19
    )

    override suspend fun load(
        context: Context,
        bounds: CoordinateBounds,
        zoom: Int,
        params: Bundle
    ): GeoJsonObject? {
        val preferences = params.getPreferences()
        val strategyId = preferences.getString(COLOR)?.toLongOrNull()
        val colorScale = ElevationColorMapFactory().getElevationColorMap(
            ElevationColorStrategy.entries.withId(strategyId ?: 0) ?: DEFAULT_COLOR
        )
        if (zoom !in minZoom..maxZoom) {
            return null
        }

        val interval = validIntervals[zoom] ?: validIntervals.values.first()
        val contours = DEM.getContourLines(
            bounds,
            interval,
            DEM.LOW_RESOLUTION_ZOOM_TO_RESOLUTION[zoom]!!
        )
        var i = -10000L

        val features = contours.flatMap { level ->
            val isImportantLine = SolMath.isZero((level.elevation / interval) % 5, 0.1f)
            val name = DecimalFormatter.format(
                Distance.meters(level.elevation).convertTo(units).value, 0
            )
            val color = colorScale.getElevationColor(level.elevation)
            level.lines.map { line ->
                GeoJsonFeature.lineString(
                    line,
                    i++,
                    name = if (isImportantLine || showLabelsOnAllContoursZoomLevels.contains(
                            zoom
                        )
                    ) {
                        name
                    } else {
                        null
                    },
                    color = color,
                    lineStyle = LineStyle.Solid,
                    thicknessScale = if (isImportantLine) {
                        0.8f
                    } else {
                        0.4f
                    }
                )
            }
        }

        return GeoJsonFeatureCollection(features)
    }

    companion object {
        const val SOURCE_ID = "contour"
        const val SHOW_LABELS = "show_labels"
        const val DEFAULT_SHOW_LABELS = true
        const val COLOR = "color"
        val DEFAULT_COLOR = ElevationColorStrategy.Brown
    }
}
