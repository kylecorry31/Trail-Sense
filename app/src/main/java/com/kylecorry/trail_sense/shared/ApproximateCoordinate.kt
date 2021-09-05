package com.kylecorry.trail_sense.shared

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

data class ApproximateCoordinate(val latitude: Double, val longitude: Double, val accuracy: Distance) {
    val coordinate = Coordinate(latitude, longitude)

    companion object {
        fun from(coordinate: Coordinate, accuracy: Distance): ApproximateCoordinate {
            return ApproximateCoordinate(coordinate.latitude, coordinate.longitude, accuracy)
        }
    }

}