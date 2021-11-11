package com.kylecorry.trail_sense.shared.hiking

import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.science.geology.IGeologyService
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.paths.PathPoint
import java.time.Duration
import kotlin.math.sqrt

class HikingService(private val geology: IGeologyService = GeologyService()) : IHikingService {

    override fun getHikingDifficulty(
        points: List<PathPoint>,
        gainThreshold: Distance
    ): HikingDifficulty {
        val gain = geology.getElevationGain(points.mapNotNull {
            if (it.elevation == null) null else Distance.meters(it.elevation)
        }, gainThreshold).convertTo(
            DistanceUnits.Feet
        ).distance

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

    override fun getHikingDuration(
        path: List<PathPoint>,
        gainThreshold: Distance,
        pace: Speed
    ): Duration {
        val speed = pace.convertTo(DistanceUnits.Meters, TimeUnits.Seconds).speed

        val gain = geology.getElevationGain(path.mapNotNull {
            if (it.elevation == null) null else Distance.meters(it.elevation)
        }, gainThreshold).meters().distance

        val distance = geology.getPathDistance(path.map { it.coordinate }).meters().distance

        val scarfs = distance + 7.92f * gain

        return Duration.ofSeconds((scarfs / speed).toLong())

    }

    override fun getHikingDuration(
        path: List<PathPoint>,
        gainThreshold: Distance,
        paceFactor: Float
    ): Duration {
        val difficulty = getHikingDifficulty(path, gainThreshold)
        return getHikingDuration(path, gainThreshold, getAveragePace(difficulty, paceFactor))
    }
}