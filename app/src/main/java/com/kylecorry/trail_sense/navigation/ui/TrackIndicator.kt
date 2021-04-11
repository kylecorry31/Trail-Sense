package com.kylecorry.trail_sense.navigation.ui

import com.kylecorry.trailsensecore.domain.geo.Coordinate
import java.time.Instant

data class Track(val points: List<Waypoint>)
data class Waypoint(val location: Coordinate, val time: Instant? = null)
