package com.kylecorry.trail_sense.navigation.paths.domain.factories

import android.graphics.Color
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.DefaultPointColoringStrategy
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.IPointColoringStrategy
import com.kylecorry.trail_sense.shared.scales.DiscreteColorScale
import com.kylecorry.trail_sense.shared.scales.IColorScale

class NonePointDisplayFactory : IPointDisplayFactory {
    override fun createColoringStrategy(path: List<PathPoint>): IPointColoringStrategy {
        return DefaultPointColoringStrategy(Color.TRANSPARENT)
    }

    override fun createColorScale(path: List<PathPoint>): IColorScale {
        return DiscreteColorScale(listOf(Color.TRANSPARENT))
    }

    override fun createLabelMap(path: List<PathPoint>): Map<Float, String> {
        return emptyMap()
    }
}