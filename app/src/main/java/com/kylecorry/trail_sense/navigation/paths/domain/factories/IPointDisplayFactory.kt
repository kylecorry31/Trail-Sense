package com.kylecorry.trail_sense.navigation.paths.domain.factories

import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors.IPointColoringStrategy

interface IPointDisplayFactory {
    fun createColoringStrategy(path: List<PathPoint>): IPointColoringStrategy
    fun createColorScale(path: List<PathPoint>): IColorScale
    fun createLabelMap(path: List<PathPoint>): Map<Float, String>
}