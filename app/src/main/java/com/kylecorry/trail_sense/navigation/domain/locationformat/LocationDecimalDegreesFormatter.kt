package com.kylecorry.trail_sense.navigation.domain.locationformat

import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trail_sense.shared.roundPlaces

class LocationDecimalDegreesFormatter : ILocationFormatter {

    override fun formatLatitude(location: Coordinate): String {
        return "${location.latitude.roundPlaces(6)}°"
    }

    override fun formatLongitude(location: Coordinate): String {
        return "${location.longitude.roundPlaces(6)}°"
    }
}