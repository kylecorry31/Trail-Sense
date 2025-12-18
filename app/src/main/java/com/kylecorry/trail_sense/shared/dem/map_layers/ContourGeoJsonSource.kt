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
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

class ContourGeoJsonSource : GeoJsonSource {

    private val units = AppServiceRegistry.get<UserPreferences>().baseDistanceUnits
    private val maxZoomLevel = 19

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

    override suspend fun load(
        bounds: CoordinateBounds,
        metersPerPixel: Float
    ): GeoJsonObject? {
        val zoomLevel = TileMath.distancePerPixelToZoom(
            metersPerPixel.toDouble(),
            (bounds.north + bounds.south) / 2
        ).coerceAtMost(maxZoomLevel)

        val interval = validIntervals[zoomLevel] ?: validIntervals.values.first()
        val contours = DEM.getContourLines(bounds, interval, validResolutions[zoomLevel]!!)
        var i = -10000L

        val features = contours.flatMap { level ->
            val isImportantLine = SolMath.isZero((level.elevation / interval) % 5, 0.1f)
            val name = DecimalFormatter.format(
                Distance.Companion.meters(level.elevation).convertTo(units).value, 0
            )
            val color = colorScale.getElevationColor(level.elevation)
            level.lines.map { line ->
                GeoJsonFeature.lineString(
                    line,
                    i++,
                    name = if (isImportantLine || showLabelsOnAllContoursZoomLevels.contains(
                            zoomLevel
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
