package com.kylecorry.trail_sense.navigation.paths.domain.factories

import android.content.Context
import android.util.Range
import com.kylecorry.andromeda.core.rangeOrNull
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.scales.ContinuousColorScale
import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.AltitudePointColoringStrategy
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.IPointColoringStrategy

class AltitudePointDisplayFactory(private val context: Context) : IPointDisplayFactory {
    override fun createColoringStrategy(path: List<PathPoint>): IPointColoringStrategy {
        val altitudeRange = getAltitudeRange(path)
        return AltitudePointColoringStrategy(
            altitudeRange,
            createColorScale(path)
        )
    }

    override fun createColorScale(path: List<PathPoint>): IColorScale {
        return ContinuousColorScale(AppColor.DarkBlue.color, AppColor.Red.color)
    }

    override fun createLabelMap(path: List<PathPoint>): Map<Float, String> {
        val range = getAltitudeRange(path)
        val units = UserPreferences(context).baseDistanceUnits
        val formatService = FormatService(context)
        val min = Distance.meters(range.lower).convertTo(units)
        val max = Distance.meters(range.upper).convertTo(units)

        // TODO: Make these at the beginning and end
        return mapOf(
            0.167f to formatService.formatDistance(min, Units.getDecimalPlaces(units), false),
            0.833f to formatService.formatDistance(max, Units.getDecimalPlaces(units), false),
        )
    }

    private fun getAltitudeRange(path: List<PathPoint>): Range<Float> {
        return path.mapNotNull { it.elevation }.rangeOrNull() ?: Range(0f, 0f)
    }
}