package com.kylecorry.trail_sense.tools.backtrack.domain.factories

import android.content.Context
import android.graphics.Color
import android.util.Range
import com.kylecorry.andromeda.core.rangeOrNull
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.scales.ContinuousColorScale
import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.IPointColoringStrategy
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.TimePointColoringStrategy
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointvalues.IPointValueStrategy
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointvalues.TimePointValueStrategy
import com.kylecorry.trail_sense.shared.paths.PathPoint
import java.time.Instant

class TimePointDisplayFactory(private val context: Context) : IPointDisplayFactory {
    override fun createColoringStrategy(path: List<PathPoint>): IPointColoringStrategy {
        val timeRange = path.mapNotNull { it.time }.rangeOrNull() ?: Range(
            Instant.now(),
            Instant.now()
        )
        return TimePointColoringStrategy(
            timeRange,
            createColorScale(path)
        )
    }

    override fun createValueStrategy(path: List<PathPoint>): IPointValueStrategy {
        return TimePointValueStrategy(context)
    }

    override fun createColorScale(path: List<PathPoint>): IColorScale {
        return ContinuousColorScale(Color.WHITE, AppColor.DarkBlue.color)
    }

    override fun createLabelMap(path: List<PathPoint>): Map<Float, String> {
        return mapOf(
            0.167f to context.getString(R.string.old),
            0.833f to context.getString(R.string.new_text),
        )
    }
}