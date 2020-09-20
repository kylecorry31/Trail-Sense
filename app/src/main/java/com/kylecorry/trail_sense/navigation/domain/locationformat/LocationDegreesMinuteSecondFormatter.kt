package com.kylecorry.trail_sense.navigation.domain.locationformat

import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trail_sense.shared.roundPlaces
import kotlin.math.abs

class LocationDegreesMinuteSecondFormatter : ILocationFormatter {

    override fun formatLatitude(location: Coordinate): String {
        val direction = if (location.latitude < 0) "S" else "N"
        return "${dmsString(location.latitude)} $direction"
    }

    override fun formatLongitude(location: Coordinate): String {
        val direction = if (location.longitude < 0) "W" else "E"
        return "${dmsString(location.longitude)} $direction"
    }

    private fun dmsString(degrees: Double): String {
        val deg = abs(degrees.toInt())
        val minutes = abs((degrees % 1) * 60)
        val seconds = abs(((minutes % 1) * 60).roundPlaces(1))
        return "$deg°${minutes.toInt()}'$seconds\""
    }
}