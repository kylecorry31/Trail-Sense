package com.kylecorry.trail_sense.tools.ballistics.domain

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.math.interpolation.Interpolator
import com.kylecorry.sol.math.interpolation.LinearInterpolator
import com.kylecorry.sol.math.optimization.ConvergenceOptimizer
import com.kylecorry.sol.math.optimization.HillClimbingOptimizer
import com.kylecorry.sol.math.optimization.IOptimizer
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.science.physics.DragModel
import com.kylecorry.sol.science.physics.Physics
import com.kylecorry.sol.science.physics.TrajectoryPoint2D
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import kotlin.math.absoluteValue


class BallisticsCalculator {

    fun calculateTrajectory(
        zeroDistance: Distance,
        scopeHeight: Distance,
        bulletSpeed: Speed,
        dragModel: DragModel
    ): List<TrajectoryPoint> {
        val stepDistance = Distance.from(
            STEP_DISTANCE_METERS_YARDS, if (zeroDistance.units.isMetric) {
                DistanceUnits.Meters
            } else {
                DistanceUnits.Yards
            }
        ).meters().value
        val maxDistance = Distance.from(
            MAX_DISTANCE_METERS_YARDS, if (zeroDistance.units.isMetric) {
                DistanceUnits.Meters
            } else {
                DistanceUnits.Yards
            }
        ).meters().value

        val initialVelocity = Physics.getVelocityVectorForImpact(
            Vector2(zeroDistance.meters().value, 0f),
            bulletSpeed.convertTo(DistanceUnits.Meters, TimeUnits.Seconds).speed,
            Vector2(0f, -scopeHeight.meters().value),
            timeStep = TIME_STEP,
            maxTime = getApproximateTime(bulletSpeed, zeroDistance).coerceAtMost(MAX_TIME),
            minAngle = MIN_ANGLE,
            maxAngle = MAX_ANGLE,
            dragModel = dragModel,
            optimizer = ConvergenceOptimizer(0.001f, 0.0001f, 0.0 to 0.0) { step, value ->
                HillClimbingOptimizer(step.toDouble(), initialValue = value)
            }
        )

        val trajectory = Physics.getTrajectory2D(
            initialPosition = Vector2(0f, -scopeHeight.meters().value),
            initialVelocity = initialVelocity,
            dragModel = dragModel,
            timeStep = TIME_STEP,
            maxTime = getApproximateTime(bulletSpeed, Distance.meters(maxDistance)).coerceAtMost(
                MAX_TIME
            )
        )

        val maxCalculatedDistance = trajectory.maxOf { it.position.x }
        val endDistance = maxDistance.coerceAtMost(maxCalculatedDistance)


        // Resample
        val timeInterpolator =
            getInterpolator(trajectory.map { Vector2(it.position.x, it.time) })
        val velocityInterpolator =
            getInterpolator(trajectory.map { Vector2(it.position.x, it.velocity.x) })
        val dropInterpolator =
            getInterpolator(trajectory.map { Vector2(it.position.x, it.position.y) })

        val newTimes =
            Interpolation.resample(timeInterpolator, 0f, endDistance, stepDistance)
        val newVelocities =
            Interpolation.resample(velocityInterpolator, 0f, endDistance, stepDistance)
        val newDrops =
            Interpolation.resample(dropInterpolator, 0f, endDistance, stepDistance)


        return newTimes.mapIndexed { index, time ->
            TrajectoryPoint(
                time.y,
                Distance.meters(time.x),
                Speed.from(
                    newVelocities[index].y,
                    DistanceUnits.Meters,
                    TimeUnits.Seconds
                ),
                Distance.meters(newDrops[index].y)
            )
        }

    }

    private fun getInterpolator(points: List<Vector2>): Interpolator {
        return LinearInterpolator(points)
    }

    private fun getApproximateTime(bulletSpeed: Speed, distance: Distance): Float {
        val speedMetersPerSecond = bulletSpeed.convertTo(
            DistanceUnits.Meters,
            TimeUnits.Seconds
        ).speed
        val distanceMeters = distance.meters().value
        val dragFactor = 1.5f
        return dragFactor * distanceMeters / speedMetersPerSecond
    }

    companion object {
        private const val MAX_DISTANCE_METERS_YARDS = 205f
        private const val STEP_DISTANCE_METERS_YARDS = 10f
        private const val MAX_TIME = 2f
        private const val TIME_STEP = 0.01f
        private const val MIN_ANGLE = 0f
        private const val MAX_ANGLE = 1f
    }

}