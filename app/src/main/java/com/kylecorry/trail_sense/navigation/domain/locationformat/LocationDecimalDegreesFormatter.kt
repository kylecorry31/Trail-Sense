package com.kylecorry.trail_sense.navigation.domain.locationformat

import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trail_sense.shared.roundPlaces

class LocationDecimalDegreesFormatter : ILocationFormatter {

    override fun format(location: Coordinate): String {
        return "${location.latitude.roundPlaces(6)}°,  ${location.longitude.roundPlaces(6)}°"
    }

}