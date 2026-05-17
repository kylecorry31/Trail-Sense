package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources

import android.content.Context
import android.os.Bundle
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds

interface GeoJsonSource {
    suspend fun load(
        context: Context,
        bounds: CoordinateBounds,
        zoom: Int,
        params: Bundle = Bundle()
    ): GeoJsonObject?

    suspend fun cleanup() {
        // Do nothing
    }
}
