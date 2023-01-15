package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.trail_sense.tools.maps.domain.sort.MapSortMethod

interface IMapPreferences {
    val areMapsEnabled: Boolean
    val autoReduceMaps: Boolean
    val showMapPreviews: Boolean
    var mapSort: MapSortMethod
}