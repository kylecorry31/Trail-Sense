package com.kylecorry.trail_sense.tools.backtrack.domain.factories

import android.content.Context
import android.graphics.Color
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.scales.DiscreteColorScale
import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.DefaultPointColoringStrategy
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.IPointColoringStrategy
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointvalues.IPointValueStrategy
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointvalues.NamePointValueStrategy
import com.kylecorry.trailsensecore.domain.geo.PathPoint

class PathDisplayFactory(private val context: Context) : IPointDisplayFactory {
    override fun createColoringStrategy(path: List<PathPoint>): IPointColoringStrategy {
        return DefaultPointColoringStrategy(UserPreferences(context).navigation.backtrackPathColor.color)
    }

    override fun createValueStrategy(path: List<PathPoint>): IPointValueStrategy {
        return NamePointValueStrategy(context)
    }

    override fun createColorScale(path: List<PathPoint>): IColorScale {
        return DiscreteColorScale(listOf(Color.TRANSPARENT))
    }

    override fun createLabelMap(path: List<PathPoint>): Map<Float, String> {
        return emptyMap()
    }
}