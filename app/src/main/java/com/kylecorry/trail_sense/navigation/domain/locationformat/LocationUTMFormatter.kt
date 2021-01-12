package com.kylecorry.trail_sense.navigation.domain.locationformat

import com.kylecorry.trailsensecore.domain.geo.Coordinate

class LocationUTMFormatter : ILocationFormatter {

    override fun format(location: Coordinate): String {
        return location.toUTM()
    }
}