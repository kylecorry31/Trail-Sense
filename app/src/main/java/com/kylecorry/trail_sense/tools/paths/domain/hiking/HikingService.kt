package com.kylecorry.trail_sense.tools.paths.domain.hiking

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
            HikingDifficulty.Easy -> Speed(1.5f * factor, DistanceUnits.Miles, TimeUnits.Hours)
            HikingDifficulty.Moderate -> Speed(1.4f * factor, DistanceUnits.Miles, TimeUnits.Hours)
            HikingDifficulty.Hard -> Speed(1.2f * factor, DistanceUnits.Miles, TimeUnits.Hours)
        }
    }

    override fun getElevationLossGain(path: List<PathPoint>): Pair<Distance, Distance> {
        val elevations =
            path.filter { it.elevation != null }.map { Distance.meters(it.elevation!!) }
        val gain = Geology.getElevationGain(elevations)
        val loss = Geology.getElevationLoss(elevations)
        return loss to gain
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
        val speed = pace.convertTo(DistanceUnits.Meters, TimeUnits.Seconds).speed
        val gain = getElevationGain(path).meters().distance

        val distance = Geology.getPathDistance(path.map { it.coordinate }).meters().distance

        val scarfs = distance + 7.92f * gain

        return Duration.ofSeconds((scarfs / speed).toLong())

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
        return Geology.getElevationGain(elevations)
    }


}