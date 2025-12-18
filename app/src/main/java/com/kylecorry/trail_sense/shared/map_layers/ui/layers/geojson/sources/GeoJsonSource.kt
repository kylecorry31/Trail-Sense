package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources

import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds

fun interface GeoJsonSource {
    suspend fun load(bounds: CoordinateBounds, metersPerPixel: Float): GeoJsonObject?
}