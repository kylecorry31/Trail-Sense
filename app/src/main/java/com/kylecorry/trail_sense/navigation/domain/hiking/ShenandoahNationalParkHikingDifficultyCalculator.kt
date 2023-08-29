package com.kylecorry.trail_sense.navigation.domain.hiking

import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import kotlin.math.sqrt

class ShenandoahNationalParkHikingDifficultyCalculator(private val hikingService: IHikingService) :
    HikingDifficultyCalculator {
    override fun calculate(points: List<PathPoint>): HikingDifficulty {
        val gain = hikingService.getElevationGain(points).convertTo(DistanceUnits.Feet).distance

        val distance =
            Geology.getPathDistance(points.map { it.coordinate })
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
}