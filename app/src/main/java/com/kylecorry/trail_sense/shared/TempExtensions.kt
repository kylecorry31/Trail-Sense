package com.kylecorry.trail_sense.shared

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trailsensecore.domain.geo.Coordinate

fun Preferences.putCoordinate(key: String, coord: Coordinate){
    putCoordinate(key, com.kylecorry.andromeda.core.units.Coordinate(coord.latitude, coord.longitude))
}