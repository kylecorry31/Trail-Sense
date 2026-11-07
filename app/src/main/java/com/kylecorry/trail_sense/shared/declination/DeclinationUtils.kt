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
     * Converts a bearing from True North to the given north reference
     * @param bearing the bearing to convert (in True North)
     * @param declination the declination in degrees
     * @param useTrueNorth true to leave the bearing in true north, false to convert it to magnetic north
     */
    fun fromTrueNorthBearing(
        bearing: Float,
        declination: Float,
        useTrueNorth: Boolean
    ): Float {
        return if (useTrueNorth) bearing else fromTrueNorthBearing(bearing, declination)
    }

    /**
     * Converts a bearing between north references
     * @param bearing the bearing to convert
     * @param declination the declination in degrees
     * @param isTrueNorth true if the bearing is in true north, false if it is in magnetic north
     * @param toTrueNorth true to convert the bearing to true north, false to convert it to magnetic north
     */
    fun convertBearing(
        bearing: Float,
        declination: Float,
        isTrueNorth: Boolean,
        toTrueNorth: Boolean
    ): Float {
        return when {
            isTrueNorth == toTrueNorth -> bearing
            toTrueNorth -> Bearing.getBearing(bearing + declination)
            else -> fromTrueNorthBearing(bearing, declination)
        }
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
