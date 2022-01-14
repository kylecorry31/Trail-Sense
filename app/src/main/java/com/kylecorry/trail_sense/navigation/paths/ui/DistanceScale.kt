package com.kylecorry.trail_sense.navigation.paths.ui

import android.graphics.Path
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits

class DistanceScale {

    fun getScaleDistance(
        units: DistanceUnits,
        maxLength: Float,
        metersPerPixel: Float
    ): Distance {
        val intervals = if (units == DistanceUnits.Meters) {
            metricScaleIntervals
        } else {
            imperialScaleIntervals
        }

        for (i in 1..intervals.lastIndex) {
            val current = intervals[i]
            val length = current.meters().distance / metersPerPixel
            if (length > maxLength) {
                return intervals[i - 1]
            }
        }

        return intervals.last()
    }

    fun getScaleBar(distance: Distance, metersPerPixel: Float, path: Path = Path()): Path {
        val length = distance.meters().distance / metersPerPixel
        val height = 12f

        // Horizontal
        path.moveTo(0f, 0f)
        path.lineTo(length, 0f)

        // Start
        path.moveTo(0f, -height / 2)
        path.lineTo(0f, height / 2)

        // End
        path.moveTo(length, -height / 2)
        path.lineTo(length, height / 2)

        // Middle
        path.moveTo(length / 2, height / 2)
        path.lineTo(length / 2, 0f)


        return path
    }


    companion object {
        private val metricScaleIntervals = listOf(
            Distance.meters(1f),
            Distance.meters(2f),
            Distance.meters(5f),
            Distance.meters(10f),
            Distance.meters(20f),
            Distance.meters(50f),
            Distance.meters(100f),
            Distance.meters(200f),
            Distance.meters(500f),
            Distance.kilometers(1f),
            Distance.kilometers(2f),
            Distance.kilometers(5f),
            Distance.kilometers(10f),
            Distance.kilometers(20f),
            Distance.kilometers(50f),
            Distance.kilometers(100f),
            Distance.kilometers(200f),
            Distance.kilometers(500f),
            Distance.kilometers(1000f),
            Distance.kilometers(2000f),
        )

        private val imperialScaleIntervals = listOf(
            Distance.feet(10f),
            Distance.feet(20f),
            Distance.feet(50f),
            Distance.feet(100f),
            Distance.feet(200f),
            Distance.feet(500f),
            Distance.miles(0.25f),
            Distance.miles(0.5f),
            Distance.miles(1f),
            Distance.miles(2f),
            Distance.miles(5f),
            Distance.miles(10f),
            Distance.miles(20f),
            Distance.miles(50f),
            Distance.miles(100f),
            Distance.miles(200f),
            Distance.miles(500f),
            Distance.miles(1000f),
            Distance.miles(2000f),
        )
    }
}