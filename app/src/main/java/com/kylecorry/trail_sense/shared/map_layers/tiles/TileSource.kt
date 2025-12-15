package com.kylecorry.trail_sense.shared.map_layers.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds

interface TileSource {
    suspend fun getRegionLoaders(bounds: CoordinateBounds): List<IGeographicImageRegionLoader>
    suspend fun getRegionLoaders(bounds: List<CoordinateBounds>): List<List<IGeographicImageRegionLoader>> {
        return bounds.map { getRegionLoaders(it) }
    }
}