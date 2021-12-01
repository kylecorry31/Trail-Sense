package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.sol.science.geology.projections.CylindricalEquidistantProjection
import com.kylecorry.sol.science.geology.projections.IMapProjection
import com.kylecorry.sol.science.geology.projections.MercatorProjection

class MapProjectionFactory {

    fun getProjection(type: MapProjectionType): IMapProjection {
        return when (type) {
            MapProjectionType.Mercator -> MercatorProjection()
            MapProjectionType.CylindricalEquidistant -> CylindricalEquidistantProjection()
        }
    }

}