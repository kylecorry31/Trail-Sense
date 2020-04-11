package com.kylecorry.trail_sense.navigation.infrastructure

import com.kylecorry.trail_sense.shared.Coordinate

interface ILocationSender {
    fun send(location: Coordinate)
}