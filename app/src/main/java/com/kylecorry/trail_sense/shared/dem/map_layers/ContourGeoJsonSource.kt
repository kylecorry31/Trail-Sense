package com.kylecorry.trail_sense.shared.dem.map_layers

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
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorMap
import com.kylecorry.trail_sense.shared.dem.colors.TrailSenseVibrantElevationColorMap
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

class ContourGeoJsonSource : GeoJsonSource {

    private val units = AppServiceRegistry.get<UserPreferences>().baseDistanceUnits

    var colorScale: ElevationColorMap = TrailSenseVibrantElevationColorMap()

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
        bounds: CoordinateBounds,
        zoom: Int
    ): GeoJsonObject? {
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
}
