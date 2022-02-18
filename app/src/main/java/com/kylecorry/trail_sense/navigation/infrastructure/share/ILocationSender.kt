package com.kylecorry.trail_sense.navigation.infrastructure.share

import com.kylecorry.andromeda.core.units.CoordinateFormat
import com.kylecorry.sol.units.Coordinate

interface ILocationSender {
    fun send(location: Coordinate, format: CoordinateFormat? = null)
}