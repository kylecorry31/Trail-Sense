package com.kylecorry.trail_sense.tools.photo_maps.infrastructure.tiles

import com.kylecorry.sol.science.geology.CoordinateBounds

interface ITileSourceSelector {
    fun getRegionLoaders(bounds: CoordinateBounds): List<IGeographicImageRegionLoader>
}