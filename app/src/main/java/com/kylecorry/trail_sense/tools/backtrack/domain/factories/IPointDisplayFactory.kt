package com.kylecorry.trail_sense.tools.backtrack.domain.factories

import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointcolors.IPointColoringStrategy
import com.kylecorry.trail_sense.tools.backtrack.domain.waypointvalues.IPointValueStrategy
import com.kylecorry.trail_sense.shared.paths.PathPoint

interface IPointDisplayFactory {
    fun createColoringStrategy(path: List<PathPoint>): IPointColoringStrategy
    fun createValueStrategy(path: List<PathPoint>): IPointValueStrategy
    fun createColorScale(path: List<PathPoint>): IColorScale
    fun createLabelMap(path: List<PathPoint>): Map<Float, String>
}