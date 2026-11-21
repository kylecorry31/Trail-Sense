package com.kylecorry.trail_sense.shared

import com.kylecorry.sol.units.Coordinate

interface GeoidService {
    /** Returns the geoid offset in meters to subtract from ellipsoid altitude. */
    suspend fun getGeoid(location: Coordinate): Float

    fun isSameGeoid(location1: Coordinate, location2: Coordinate): Boolean
}
