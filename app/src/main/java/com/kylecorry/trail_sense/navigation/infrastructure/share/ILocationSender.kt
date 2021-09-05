package com.kylecorry.trail_sense.navigation.infrastructure.share

import com.kylecorry.sol.units.Coordinate

interface ILocationSender {
    fun send(location: Coordinate)
}