package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.trailsensecore.domain.geo.Coordinate

data class MapCalibrationPoint(val location: Coordinate, val imageLocation: PercentCoordinate)
