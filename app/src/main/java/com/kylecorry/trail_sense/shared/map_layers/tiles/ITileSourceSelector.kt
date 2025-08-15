package com.kylecorry.trail_sense.shared.map_layers.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds

interface ITileSourceSelector {
    fun getRegionLoaders(bounds: CoordinateBounds): List<IGeographicImageRegionLoader>
}