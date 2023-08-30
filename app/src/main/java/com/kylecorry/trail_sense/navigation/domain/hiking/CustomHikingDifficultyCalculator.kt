package com.kylecorry.trail_sense.navigation.domain.hiking

import android.util.Log
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.filters.RDPFilter
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.extensions.ifDebug
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class CustomHikingDifficultyCalculator(private val hikingService: IHikingService) :
    HikingDifficultyCalculator {
    override fun calculate(points: List<PathPoint>): HikingDifficulty {
        val simplified = simplify(points)

        // Gain / Loss
        val gainLoss = hikingService.getElevationLossGain(simplified)
        val loss = gainLoss.first.meters()
        val gain = gainLoss.second.meters()

        // Distance
        val distance = Geology.getPathDistance(simplified.map { it.coordinate }).meters()

        // Slopes
        val slopes = hikingService.getSlopes(simplified).map { it.third }
        val maxSlope = slopes.maxByOrNull { it.absoluteValue }?.absoluteValue ?: 0f

        val isOutAndBack = if (gain.distance > 0f) {
            abs(gain.distance + loss.distance) / gain.distance < 0.1
        } else {
            false
        }

        val averageSlopeDistance = if (isOutAndBack){
            Distance.meters(distance.distance / 2f)
        } else {
            distance
        }

        val averageUphillSlope = Geology.getSlopeGrade(averageSlopeDistance, gain)
        val averageDownhillSlope = Geology.getSlopeGrade(averageSlopeDistance, loss)

        // Map each factor between 0 and 1
        val factors = listOf(
            // A short hike is easier than a long hike
            SolMath.norm(distance.distance, 0f, 15000f),
            // A steep area can make a hike more difficult
            SolMath.norm(maxSlope, 0f, 40f),
            // An uphill hike is more difficult than flat
            SolMath.norm(averageUphillSlope, 0f, 35f),
            // A downhill hike is more difficult than flat, but less than uphill
            SolMath.norm(-averageDownhillSlope, 0f, 45f)
        ).map { (it * 100).roundToInt() }

        ifDebug {
            Log.d("HikingDifficulty", "Factors: $factors")
        }

        val maxFactor = factors.maxOrNull() ?: 0

        return when {
            maxFactor < 40 -> HikingDifficulty.Easiest
            maxFactor < 50 -> HikingDifficulty.Moderate
            maxFactor < 60 -> HikingDifficulty.ModeratelyStrenuous
            maxFactor < 80 -> HikingDifficulty.Strenuous
            else -> HikingDifficulty.VeryStrenuous
        }
    }

    private fun simplify(path: List<PathPoint>): List<PathPoint> {
        val epsilon = 4f // Medium
        val filter = RDPFilter<PathPoint>(epsilon) { point, start, end ->
            Geology.getCrossTrackDistance(
                point.coordinate,
                start.coordinate,
                end.coordinate
            ).distance.absoluteValue
        }
        return filter.filter(path)
    }
}