package com.kylecorry.trail_sense.tools.navigation.infrastructure.share

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat

interface ILocationSender {
    fun send(location: Coordinate, format: BuiltInCoordinateFormat? = null)
}