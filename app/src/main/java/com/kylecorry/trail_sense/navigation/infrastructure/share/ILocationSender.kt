package com.kylecorry.trail_sense.navigation.infrastructure.share

import com.kylecorry.trailsensecore.domain.Coordinate

interface ILocationSender {
    fun send(location: Coordinate)
}