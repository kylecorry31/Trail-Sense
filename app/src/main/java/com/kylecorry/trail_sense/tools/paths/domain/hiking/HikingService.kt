package com.kylecorry.trail_sense.tools.paths.domain.hiking

import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import java.time.Duration

class HikingService : IHikingService {

    override fun getDistances(points: List<Coordinate>, minDistance: Float): List<Float> {
        if (points.isEmpty()) {
            return emptyList()
        }
        var distance = 0f
        var last = points.first()

        return points.map {
            distance += it.distanceTo(last).coerceAtLeast(minDistance)
            last = it
            distance
        }
    }

    override fun correctElevations(points: List<PathPoint>): List<PathPoint> {
        if (points.isEmpty()) {
            return emptyList()
        }
        val smoothed = DataUtils.smoothGeospatial(
            points,
            0.1f,
            DataUtils.GeospatialSmoothingType.Path,
            { it.coordinate },
            { it.elevation ?: 0f }
        ) { point, smoothed ->
            point.copy(elevation = if (point.elevation == null) null else smoothed)
        }
        return smoothed
    }

    override fun getHikingDifficulty(points: List<PathPoint>): HikingDifficulty {
        val calculator = SimpleHikingDifficultyCalculator(this)
        return calculator.calculate(points)
    }

    override fun getAveragePace(difficulty: HikingDifficulty, factor: Float): Speed {
        return when (difficulty) {
            HikingDifficulty.Easy -> Speed.from(1.5f * factor, DistanceUnits.Miles, TimeUnits.Hours)
            HikingDifficulty.Moderate -> Speed.from(
                1.4f * factor,
                DistanceUnits.Miles,
                TimeUnits.Hours
            )

            HikingDifficulty.Hard -> Speed.from(1.2f * factor, DistanceUnits.Miles, TimeUnits.Hours)
        }
    }

    fun getElevations(path: List<PathPoint>): FloatArray {
        val elevations = FloatArray(path.size)
        var firstElevationIndex = -1
        for (i in path.indices) {
            if (firstElevationIndex == -1 && path[i].elevation != null) {
                firstElevationIndex = i
            }
            elevations[i] = path[i].elevation ?: elevations.getOrElse(i - 1) { 0f }
        }
        if (firstElevationIndex == -1) {
            return FloatArray(path.size)
        }

        // Fill the missing elevations prior to the first known elevation with the first known elevation
        for (i in 0 until firstElevationIndex) {
            elevations[i] = elevations[firstElevationIndex]
        }
        return elevations
    }

    override fun getElevationLossGain(path: List<PathPoint>): Pair<Distance, Distance> {
        val elevations = getElevations(path)
        val (losses, gains) = getCumulativeElevationLossGain(elevations)
        return Distance.meters(losses.lastOrNull() ?: 0f) to Distance.meters(gains.lastOrNull() ?: 0f)
    }

    fun getCumulativeElevationLossGain(path: List<PathPoint>): Pair<FloatArray, FloatArray> {
        val elevations = getElevations(path)
        return getCumulativeElevationLossGain(elevations)
    }

    fun getCumulativeElevationLossGain(elevations: FloatArray): Pair<FloatArray, FloatArray> {
        if (elevations.isEmpty()) {
            return FloatArray(0) to FloatArray(0)
        }
        val gains = FloatArray(elevations.size)
        val losses = FloatArray(elevations.size)

        for (i in 1..<elevations.size) {
            val current = elevations[i]
            val last = elevations[i - 1]
            val change = current - last
            gains[i] = gains[i - 1]
            losses[i] = losses[i - 1]
            if (change > 0) {
                gains[i] = change + gains[i - 1]
            } else if (change < 0) {
                losses[i] = change + losses[i - 1]
            }
        }
        return losses to gains
    }

    override fun getSlopes(path: List<PathPoint>): List<Triple<PathPoint, PathPoint, Float>> {
        return path.zipWithNext()
            .map {
                Triple(it.first, it.second, getSlope(it.first, it.second))
            }
    }

    private fun getSlope(a: PathPoint, b: PathPoint): Float {
        return Geology.getSlopeGrade(
            a.coordinate, Distance.meters(a.elevation ?: 0f),
            b.coordinate, Distance.meters(b.elevation ?: 0f)
        )
    }

    override fun getHikingDuration(
        path: List<PathPoint>,
        pace: Speed
    ): Duration {
        val gain = getElevationGain(path)
        val distance = Geography.getPathDistance(path.map { it.coordinate })
        return getHikingDuration(distance, gain, pace)
    }

    fun getHikingDuration(
        distance: Distance,
        elevationGain: Distance,
        speed: Speed
    ): Duration {
        val speedValue = speed.convertTo(DistanceUnits.Meters, TimeUnits.Seconds).value.coerceAtLeast(0.1f)
        val scarfs = distance.meters().value + 7.92f * elevationGain.meters().value
        return Duration.ofSeconds((scarfs / speedValue).toLong())
    }

    override fun getHikingDuration(
        path: List<PathPoint>,
        paceFactor: Float,
        difficulty: HikingDifficulty?
    ): Duration {
        val diff = difficulty ?: getHikingDifficulty(path)
        return getHikingDuration(path, getAveragePace(diff, paceFactor))
    }

    override fun getElevationGain(path: List<PathPoint>): Distance {
        val elevations =
            path.filter { it.elevation != null }.map { Distance.meters(it.elevation!!) }
        return Geography.getElevationGain(elevations)
    }

    companion object {
        const val DEFAULT_PACE_FACTOR = 1.75f
    }
}
