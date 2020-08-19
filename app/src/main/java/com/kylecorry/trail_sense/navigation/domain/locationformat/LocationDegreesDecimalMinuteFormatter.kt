package com.kylecorry.trail_sense.navigation.domain.locationformat

import com.kylecorry.trail_sense.shared.domain.Coordinate
import com.kylecorry.trail_sense.shared.roundPlaces
import kotlin.math.abs

class LocationDegreesDecimalMinuteFormatter : ILocationFormatter {

    override fun formatLatitude(location: Coordinate): String {
        val direction = if (location.latitude < 0) "S" else "N"
        return "${format(location.latitude)} $direction"
    }

    override fun formatLongitude(location: Coordinate): String {
        val direction = if (location.longitude < 0) "W" else "E"
        return "${format(location.longitude)} $direction"
    }

    private fun format(degrees: Double): String {
        val deg = abs(degrees.toInt())
        val minutes = abs((degrees % 1) * 60).roundPlaces(3)
        return "$deg°$minutes'"
    }
}