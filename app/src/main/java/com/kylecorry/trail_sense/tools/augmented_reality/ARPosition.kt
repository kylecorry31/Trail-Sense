package com.kylecorry.trail_sense.tools.augmented_reality

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils

class ARPosition private constructor(
    private val horizon: AugmentedRealityView.HorizonCoordinate?,
    private val coordinate: Coordinate?,
    private val altitude: Float?,
) {

    fun getHorizonCoordinate(
        myLocation: Coordinate,
        myElevation: Float
    ): AugmentedRealityView.HorizonCoordinate {
        return horizon ?: coordinate?.let {
            AugmentedRealityUtils.getHorizonCoordinate(myLocation, myElevation, it, altitude)
        } ?: AugmentedRealityView.HorizonCoordinate(0f, 0f)
    }

    companion object {
        fun horizon(horizon: AugmentedRealityView.HorizonCoordinate): ARPosition {
            return ARPosition(horizon, null, null)
        }

        fun geographic(coordinate: Coordinate, altitude: Float? = null): ARPosition {
            return ARPosition(null, coordinate, altitude)
        }
    }

}