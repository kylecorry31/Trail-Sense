package com.kylecorry.trail_sense.navigation.infrastructure

import com.kylecorry.trail_sense.shared.Coordinate

internal data class PathWayPointDto(val pathName: String, val order: Int, val location: Coordinate)