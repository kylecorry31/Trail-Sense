package com.kylecorry.trail_sense.tools.ballistics.domain

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.abs

class BallisticsCalculatorTest {

    // Source of truth: https://shooterscalculator.com/ballistic-trajectory-chart.php
    @ParameterizedTest
    @CsvSource(
        "22 LR, 1200, 0.135, -6.02, -20.92, -45.97",
        "223 Remington, 2850, 0.371, 0.31, -0.65, -3.03",
        "308 Winchester, 2700, 0.475, 0.2, -0.96, -3.62",
        "JSB 18.13 Pellet, 900, 0.035, -14.57, -52.2, -122.83"
    )
    fun calculateTrajectory(
        name: String,
        muzzleVelocity: Float,
        bc: Float,
        drop100: Float,
        drop150: Float,
        drop200: Float
    ) {
        val scopeHeight = Distance.from(1.5f, DistanceUnits.Inches)
        val zeroRange = Distance.from(50f, DistanceUnits.Yards)
        val dragModel = G1DragModel(bc)
        val actual = BallisticsCalculator().calculateTrajectory(
            zeroRange,
            scopeHeight,
            Speed.from(muzzleVelocity, DistanceUnits.Feet, TimeUnits.Seconds),
            dragModel
        )
        val actualDrop50 = getNearestTrajectoryPoint(actual, 50f).drop.convertTo(
            DistanceUnits.Inches
        ).value
        val actualDrop100 = getNearestTrajectoryPoint(actual, 100f).drop.convertTo(
            DistanceUnits.Inches
        ).value
        val actualDrop150 = getNearestTrajectoryPoint(actual, 150f).drop.convertTo(
            DistanceUnits.Inches
        ).value
        val actualDrop200 = getNearestTrajectoryPoint(actual, 200f).drop.convertTo(
            DistanceUnits.Inches
        ).value

        assertEquals(0f, actualDrop50, 0.05f, "Drop at 50 yards for $name")
        assertEquals(drop100, actualDrop100, 0.1f, "Drop at 100 yards for $name")
        assertEquals(drop150, actualDrop150, 0.1f, "Drop at 150 yards for $name")
        assertEquals(drop200, actualDrop200, 0.25f, "Drop at 200 yards for $name")

        val errors = listOf(
            abs(actualDrop50),
            abs(drop100 - actualDrop100),
            abs(drop150 - actualDrop150),
            abs(drop200 - actualDrop200)
        )
        println("Trajectory errors for $name: $errors")

    }

    private fun getNearestTrajectoryPoint(
        trajectory: List<TrajectoryPoint>,
        distanceYards: Float,
    ): TrajectoryPoint {
        return trajectory.minBy { abs(it.distance.convertTo(DistanceUnits.Yards).value - distanceYards) }
    }

}