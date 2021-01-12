package com.kylecorry.trail_sense.navigation.domain.locationformat

import com.kylecorry.trailsensecore.domain.geo.Coordinate

interface ILocationFormatter {

    fun format(location: Coordinate): String

}