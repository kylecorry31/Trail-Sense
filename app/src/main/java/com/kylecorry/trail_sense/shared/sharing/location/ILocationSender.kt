package com.kylecorry.trail_sense.shared.sharing.location

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat

interface ILocationSender {
    fun send(location: Coordinate, format: BuiltInCoordinateFormat? = null)
}
