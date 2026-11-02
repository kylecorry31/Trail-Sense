package com.kylecorry.trail_sense.tools.celestial_navigation.infrastructure

import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.science.geophysics.VerticalDeflection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.AltitudeCorrection

/**
 * Vertical deflection from the slope of the built-in geoid
 */
object DeflectionModels {

    private const val STEP_DEGREES = 0.5

    suspend fun getDeflection(location: Coordinate, index: Int): VerticalDeflection? {
        if (index != 1) return null
        // Sol's geoid lookup isn't suspending, so sample the points it uses first
        val points = listOf(
            Coordinate(location.latitude + STEP_DEGREES, location.longitude),
            Coordinate(location.latitude - STEP_DEGREES, location.longitude),
            Coordinate(location.latitude, location.longitude + STEP_DEGREES),
            Coordinate(location.latitude, location.longitude - STEP_DEGREES)
        )
        val geoid = points.associateWith { AltitudeCorrection.getGeoid(it).toDouble() }
        return Geophysics.getVerticalDeflectionModel(STEP_DEGREES) { point ->
            geoid[point] ?: geoid.minBy { it.key.distanceTo(point) }.value
        }.getVerticalDeflection(location)
    }

    /**
     * Converts an astronomic location (from gravity) to geodetic
     */
    suspend fun correct(astronomic: Coordinate, index: Int): Coordinate {
        val deflection = getDeflection(astronomic, index) ?: return astronomic
        return Geophysics.getGeodeticLocation(astronomic, deflection)
    }

    suspend fun describe(location: Coordinate, index: Int): String {
        val deflection = getDeflection(location, index) ?: return "none"
        return "xi=%.2f\" eta=%.2f\"".format(deflection.xi, deflection.eta)
    }
}
