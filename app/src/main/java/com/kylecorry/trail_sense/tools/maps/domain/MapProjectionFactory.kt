package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.projections.IMapProjection
import com.kylecorry.sol.science.geology.projections.MercatorMapProjection

class MapProjectionFactory(private val bounds: CoordinateBounds, private val size: Size) {

    fun getProjection(type: MapProjectionType): IMapProjection {
        return when (type) {
            MapProjectionType.TransverseMercator -> MercatorMapProjection(bounds, size)
            MapProjectionType.Equirectangular -> EquirectangularProjection(bounds, size)
        }
    }

}