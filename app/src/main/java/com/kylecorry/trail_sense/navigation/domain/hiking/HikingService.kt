package com.kylecorry.trail_sense.navigation.domain.hiking

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.science.geology.IGeologyService
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.data.DataUtils
import java.time.Duration
import kotlin.math.sqrt

class HikingService(private val geology: IGeologyService = GeologyService()) : IHikingService {

    override fun getDistances(points: List<PathPoint>): List<Float> {
        if (points.isEmpty()){
            return emptyList()
        }
        var distance = 0f
        var last = points.first()

        return points.map {
            distance += it.coordinate.distanceTo(last.coordinate)
            last = it
            distance
        }
    }

    override fun correctElevations(points: List<PathPoint>): List<PathPoint> {
        if (points.isEmpty()){
            return emptyList()
        }
        val distances = getDistances(points)
        val smoothed = DataUtils.smooth(
            points,
            0.1f,
            { index, value -> Vector2(distances[index], value.elevation ?: 0f) }
        ) { point, smoothed ->
            point.copy(elevation = if (point.elevation == null) null else smoothed.y)
        }
        return smoothed
    }

    override fun getHikingDifficulty(points: List<PathPoint>): HikingDifficulty {
        val gain = getElevationGain(points).convertTo(DistanceUnits.Feet).distance

        val distance =
            geology.getPathDistance(points.map { it.coordinate })
                .convertTo(DistanceUnits.Miles).distance

        val rating = sqrt(gain * 2 * distance)

        return when {
            rating < 50 -> HikingDifficulty.Easiest
            rating < 100 -> HikingDifficulty.Moderate
            rating < 150 -> HikingDifficulty.ModeratelyStrenuous
            rating < 200 -> HikingDifficulty.Strenuous
            else -> HikingDifficulty.VeryStrenuous
        }

    }

    override fun getAveragePace(difficulty: HikingDifficulty, factor: Float): Speed {
        return when (difficulty) {
            HikingDifficulty.Easiest -> Speed(1.5f * factor, DistanceUnits.Miles, TimeUnits.Hours)
            HikingDifficulty.Moderate -> Speed(1.4f * factor, DistanceUnits.Miles, TimeUnits.Hours)
            HikingDifficulty.ModeratelyStrenuous -> Speed(
                1.3f * factor,
                DistanceUnits.Miles,
                TimeUnits.Hours
            )
            HikingDifficulty.Strenuous -> Speed(1.2f * factor, DistanceUnits.Miles, TimeUnits.Hours)
            HikingDifficulty.VeryStrenuous -> Speed(
                1.2f * factor,
                DistanceUnits.Miles,
                TimeUnits.Hours
            )
        }
    }

    override fun getElevationLossGain(path: List<PathPoint>): Pair<Distance, Distance> {
        val elevations =
            path.filter { it.elevation != null }.map { Distance.meters(it.elevation!!) }
        val gain = geology.getElevationGain(elevations)
        val loss = geology.getElevationLoss(elevations)
        return loss to gain
    }

    override fun getSlopes(path: List<PathPoint>): List<Triple<PathPoint, PathPoint, Float>> {
        return path.zipWithNext()
            .map {
                Triple(it.first, it.second, getSlope(it.first, it.second))
            }
    }

    private fun getSlope(a: PathPoint, b: PathPoint): Float {
        return geology.getSlopeGrade(
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

        val distance = geology.getPathDistance(path.map { it.coordinate }).meters().distance

        val scarfs = distance + 7.92f * gain

        return Duration.ofSeconds((scarfs / speed).toLong())

    }

    override fun getHikingDuration(
        path: List<PathPoint>,
        paceFactor: Float
    ): Duration {
        val difficulty = getHikingDifficulty(path)
        return getHikingDuration(path, getAveragePace(difficulty, paceFactor))
    }

    private fun getElevationGain(path: List<PathPoint>): Distance {
        val elevations =
            path.filter { it.elevation != null }.map { Distance.meters(it.elevation!!) }
        return geology.getElevationGain(elevations)
    }


}