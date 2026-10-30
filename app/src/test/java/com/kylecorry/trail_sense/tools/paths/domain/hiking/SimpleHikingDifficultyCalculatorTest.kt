package com.kylecorry.trail_sense.tools.paths.domain.hiking

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SimpleHikingDifficultyCalculatorTest {

    private val calculator = SimpleHikingDifficultyCalculator

    @Test
    fun aShortFlatPathIsEasy() {
        assertEquals(HikingDifficulty.Easy, calculator.calculate(path(0.001, 0f, 0f, 0f)))
    }

    @Test
    fun twoMilesIsModerate() {
        // 2 legs of 0.015 degrees (1.67 km) is 2.07 miles
        assertEquals(HikingDifficulty.Moderate, calculator.calculate(path(0.015, 0f, 0f, 0f)))
    }

    @Test
    fun fourMilesIsHard() {
        // 2 legs of 0.03 degrees (3.34 km) is 4.15 miles
        assertEquals(HikingDifficulty.Hard, calculator.calculate(path(0.03, 0f, 0f, 0f)))
    }

    @Test
    fun climbingOverTwoHundredFiftyFeetIsModerate() {
        // 100 m is 328 ft over 1.38 miles at a 4.5% grade
        assertEquals(HikingDifficulty.Moderate, calculator.calculate(path(0.01, 0f, 50f, 100f)))
    }

    @Test
    fun climbingOverSevenHundredFiftyFeetIsHard() {
        // 240 m is 787 ft over 1.38 miles at a 10.8% grade
        assertEquals(HikingDifficulty.Hard, calculator.calculate(path(0.01, 0f, 120f, 240f)))
    }

    @Test
    fun descendingCountsAsMuchAsClimbing() {
        assertEquals(HikingDifficulty.Hard, calculator.calculate(path(0.01, 240f, 120f, 0f)))
    }

    @Test
    fun aSteepSlopeMakesAShortPathModerateOrHard() {
        // Each leg is 111 m long
        assertEquals(HikingDifficulty.Moderate, calculator.calculate(path(0.001, 0f, 17f, 17f))) // 15.3%
        assertEquals(HikingDifficulty.Hard, calculator.calculate(path(0.001, 0f, 28f, 28f))) // 25.2%
    }

    private fun path(legDegrees: Double, vararg elevations: Float): List<PathPoint> {
        return elevations.mapIndexed { index, elevation ->
            PathPoint(index.toLong(), 1, Coordinate(index * legDegrees, 0.0), elevation)
        }
    }
}
