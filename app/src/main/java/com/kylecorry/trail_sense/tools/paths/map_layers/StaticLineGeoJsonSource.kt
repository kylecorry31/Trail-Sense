package com.kylecorry.trail_sense.tools.paths.map_layers

import android.content.Context
import android.os.Bundle
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

class StaticLineGeoJsonSource(
    @Volatile var line: List<Coordinate>,
    private val color: Int,
    private val lineStyle: LineStyle = LineStyle.Solid,
    private val thicknessScale: Float = 1f
) : GeoJsonSource {
    override suspend fun load(
        context: Context,
        bounds: CoordinateBounds,
        zoom: Int,
        params: Bundle
    ): GeoJsonObject {
        val points = line
        if (points.size < 2) {
            return GeoJsonFeatureCollection(emptyList())
        }
        return GeoJsonFeatureCollection(
            listOf(
                GeoJsonFeature.lineString(
                    points,
                    color = color,
                    lineStyle = lineStyle,
                    thicknessScale = thicknessScale
                )
            )
        )
    }
}
