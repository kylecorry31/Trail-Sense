package com.kylecorry.trail_sense.tools.photo_maps.domain

import com.kylecorry.sol.science.geography.projections.CylindricalEquidistantProjection
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.science.geography.projections.MercatorProjection

class MapProjectionFactory {

    fun getProjection(type: MapProjectionType): IMapProjection {
        return when (type) {
            MapProjectionType.Mercator -> MercatorProjection()
            MapProjectionType.CylindricalEquidistant -> CylindricalEquidistantProjection()
        }
    }

}