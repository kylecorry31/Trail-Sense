package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources

import android.os.Bundle
import androidx.core.os.bundleOf
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds

interface GeoJsonSource {
    suspend fun load(
        bounds: CoordinateBounds,
        zoom: Int,
        params: Bundle = bundleOf()
    ): GeoJsonObject?

    companion object {
        const val PARAM_TIME = "time"
        const val PARAM_PREFERENCES = "preferences"
    }
}
