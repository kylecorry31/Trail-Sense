package com.kylecorry.trail_sense.tools.paths.map_layers

import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath

class PathGeoJsonSource : GeoJsonSource {

    var paths: List<IMappablePath> = emptyList()

    override suspend fun load(
        bounds: CoordinateBounds,
        metersPerPixel: Float
    ): GeoJsonObject? {
        return GeoJsonFeatureCollection(paths.map {
            GeoJsonFeature.lineString(
                it.points.map { point -> point.coordinate },
                it.id,
                it.name,
                it.style,
                it.color,
                it.thicknessScale
            )
        })
    }
}