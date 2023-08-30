package com.kylecorry.trail_sense.navigation.domain.hiking

import android.util.Log
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.filters.RDPFilter
import com.kylecorry.sol.math.sumOfFloat
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.extensions.ifDebug
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class CustomHikingDifficultyCalculator(private val hikingService: IHikingService) :
    HikingDifficultyCalculator {
    override fun calculate(points: List<PathPoint>): HikingDifficulty {
        // Gain / Loss
        val gainLoss = hikingService.getElevationLossGain(points)
        val loss = gainLoss.first.convertTo(DistanceUnits.Feet)
        val gain = gainLoss.second.convertTo(DistanceUnits.Feet)

        // Distance
        val distance =
            Geology.getPathDistance(points.map { it.coordinate }).convertTo(DistanceUnits.Miles)

        // Slopes
        val slopes = hikingService.getSlopes(points)

        var uphillSteepDistance = 0f
        var downhillSteepDistance = 0f

        slopes.forEach {
            if (it.third > 25f) {
                uphillSteepDistance += it.first.coordinate.distanceTo(it.second.coordinate)
            } else if (it.third < -25f) {
                downhillSteepDistance += it.first.coordinate.distanceTo(it.second.coordinate)
            }
        }

        // Convert to feet
        uphillSteepDistance =
            Distance.meters(uphillSteepDistance).convertTo(DistanceUnits.Feet).distance
        downhillSteepDistance =
            Distance.meters(downhillSteepDistance).convertTo(DistanceUnits.Feet).distance

        // Modified Petzoldt energy miles formula - factors in elevation loss and steep sections
        val energyMiles =
            distance.distance + // Longer distance is harder
                    gain.distance / 500f - // Uphill is harder
                    loss.distance / 1000f + // Downhill is a little harder
                    uphillSteepDistance / 100f + // Steep sections are harder
                    downhillSteepDistance / 200f // Steep sections are harder

        ifDebug {
            Log.d("HikingDifficulty", "Energy miles: $energyMiles")
        }

        return when {
            energyMiles < 5 -> HikingDifficulty.Easy
            energyMiles < 8 -> HikingDifficulty.Moderate
            else -> HikingDifficulty.Hard
        }
    }

}