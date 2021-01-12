package com.kylecorry.trail_sense.navigation.domain.locationformat

import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trail_sense.shared.roundPlaces
import kotlin.math.abs

class LocationDegreesDecimalMinuteFormatter : ILocationFormatter {

    override fun format(location: Coordinate): String {
        return location.toDegreeDecimalMinutes(3)
    }
}