package com.kylecorry.trail_sense.tools.navigation.infrastructure.share

import com.kylecorry.sol.science.geography.CoordinateFormat
import com.kylecorry.sol.units.Coordinate

interface ILocationSender {
    fun send(location: Coordinate, format: CoordinateFormat? = null)
}