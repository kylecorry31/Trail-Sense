package com.kylecorry.trail_sense.shared.declination

import com.kylecorry.sol.units.Bearing

object DeclinationUtils {

    /**
     * Converts a bearing from True North
     * @param bearing the bearing to convert (in True North)
     * @param declination the declination in degrees
     * @return the bearing in magnetic north
     */
    fun fromTrueNorthBearing(
        bearing: Float,
        declination: Float
    ): Float {
        return Bearing.getBearing(bearing - declination)
    }

    /**
     * Converts a bearing from True North
     * @param bearing the bearing to convert (in True North)
     * @param declination the declination in degrees
     * @return the bearing in magnetic north
     */
    fun fromTrueNorthBearing(
        bearing: Bearing,
        declination: Float
    ): Bearing {
        return bearing.withDeclination(-declination)
    }

}