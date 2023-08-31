package com.kylecorry.trail_sense.navigation.domain.hiking

import android.util.Log
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.extensions.ifDebug
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
            ifDebug {
                Log.d("HikingDifficulty", "Distance: $distance (Hard)")
            }
            return HikingDifficulty.Hard
        }

        // Gain / Loss
        val gainLoss = hikingService.getElevationLossGain(points)
        val loss = gainLoss.first.convertTo(DistanceUnits.Feet).distance
        val gain = gainLoss.second.convertTo(DistanceUnits.Feet).distance
        val elevationChange = max(gain, -loss)

        if (elevationChange >= 750) {
            ifDebug {
                Log.d(
                    "HikingDifficulty",
                    "Distance: $distance, Elevation change: $elevationChange (Hard)"
                )
            }
            return HikingDifficulty.Hard
        }

        // Slopes
        val slopes = hikingService.getSlopes(points)
        val maxSlope = slopes.maxByOrNull { it.third.absoluteValue }?.third?.absoluteValue ?: 0f

        val rating = if (maxSlope >= 25) {
            HikingDifficulty.Hard
        } else if (elevationChange >= 250 || distance >= 2) {
            HikingDifficulty.Moderate
        } else {
            HikingDifficulty.Easy
        }

        ifDebug {
            Log.d(
                "HikingDifficulty",
                "Distance: $distance, Elevation change: $elevationChange, Max slope: $maxSlope ($rating)"
            )
        }

        return rating
    }
}