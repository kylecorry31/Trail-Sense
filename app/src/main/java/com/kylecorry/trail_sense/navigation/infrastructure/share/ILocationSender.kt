package com.kylecorry.trail_sense.navigation.infrastructure.share

import com.kylecorry.trailsensecore.domain.geo.Coordinate

interface ILocationSender {
    fun send(location: Coordinate)
}