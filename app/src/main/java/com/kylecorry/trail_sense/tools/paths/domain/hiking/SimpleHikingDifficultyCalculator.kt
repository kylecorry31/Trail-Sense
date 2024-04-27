package com.kylecorry.trail_sense.tools.paths.domain.hiking

import android.util.Log
import com.kylecorry.sol.math.SolMath.roundPlaces
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.debugging.ifDebug
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import kotlin.math.absoluteValue
import kotlin.math.max

// Adapted from https://www.smcgov.org/parks/county-hiking-trail-difficulty-rating-system
class SimpleHikingDifficultyCalculator(private val hikingService: IHikingService) :
    HikingDifficultyCalculator {

    override fun calculate(points: List<PathPoint>): HikingDifficulty {
        // Distance
        val distance = Geology.getPathDistance(points.map { it.coordinate })
            .convertTo(DistanceUnits.Miles)
            .distance

        if (distance >= 4) {
            debugLog(HikingDifficulty.Hard, distance, null, null)
            return HikingDifficulty.Hard
        }

        // Gain / Loss
        val gainLoss = hikingService.getElevationLossGain(points)
        val loss = gainLoss.first.convertTo(DistanceUnits.Feet).distance
        val gain = gainLoss.second.convertTo(DistanceUnits.Feet).distance
        val elevationChange = max(gain, -loss)

        if (elevationChange >= 750) {
            debugLog(HikingDifficulty.Hard, distance, elevationChange, null)
            return HikingDifficulty.Hard
        }

        // Slopes
        val slopes = hikingService.getSlopes(points)
        val maxSlope = slopes.maxByOrNull { it.third.absoluteValue }?.third?.absoluteValue ?: 0f

        val rating = if (maxSlope >= 25) {
            HikingDifficulty.Hard
        } else if (elevationChange >= 250 || distance >= 2 || maxSlope >= 15) {
            HikingDifficulty.Moderate
        } else {
            HikingDifficulty.Easy
        }

        debugLog(rating, distance, elevationChange, maxSlope)

        return rating
    }

    private fun debugLog(
        rating: HikingDifficulty,
        distance: Float?,
        elevationChange: Float?,
        slope: Float?
    ) {
        ifDebug {
            Log.d(
                "HikingDifficulty",
                "Dist: ${distance?.roundPlaces(2)}, Ele: ${elevationChange?.safeRoundToInt()}, Slope: ${slope?.safeRoundToInt()} ($rating)"
            )
        }
    }
}