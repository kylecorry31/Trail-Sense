package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.trail_sense.tools.maps.domain.sort.MapSortMethod

interface IMapPreferences {
    val autoReducePhotoMaps: Boolean
    val autoReducePdfMaps: Boolean
    val showMapPreviews: Boolean
    var mapSort: MapSortMethod
    val keepMapFacingUp: Boolean
}