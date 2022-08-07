package com.kylecorry.trail_sense.navigation.paths.domain.factories

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.IPointColoringStrategy
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.SlopePointColoringStrategy
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.scales.DiscreteColorScale
import com.kylecorry.trail_sense.shared.scales.IColorScale

class SlopePointDisplayFactory(private val context: Context) : IPointDisplayFactory {
    override fun createColoringStrategy(path: List<PathPoint>): IPointColoringStrategy {
        return SlopePointColoringStrategy(
            createColorScale(path)
        )
    }

    override fun createColorScale(path: List<PathPoint>): IColorScale {
        return DiscreteColorScale(
            listOf(
                AppColor.Green.color,
                AppColor.Yellow.color,
                AppColor.Red.color
            )
        )
    }

    override fun createLabelMap(path: List<PathPoint>): Map<Float, String> {
        return mapOf(
            0.167f to context.getString(R.string.path_slope_flat),
            0.5f to context.getString(R.string.path_slope_moderate),
            0.833f to context.getString(R.string.path_slope_steep),
        )
    }
}