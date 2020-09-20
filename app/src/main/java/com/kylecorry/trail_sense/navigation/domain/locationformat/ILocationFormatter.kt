package com.kylecorry.trail_sense.navigation.domain.locationformat

import com.kylecorry.trailsensecore.domain.geo.Coordinate

interface ILocationFormatter {

    fun formatLatitude(location: Coordinate): String
    fun formatLongitude(location: Coordinate): String

}