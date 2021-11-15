package com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors

import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint

class SelectedPointDecorator(
    private val selectedPointId: Long,
    private val selectedColoringStrategy: IPointColoringStrategy,
    private val deselectedColoringStrategy: IPointColoringStrategy
) : IPointColoringStrategy {
    override fun getColor(point: PathPoint): Int? {
        return if (point.id == selectedPointId) {
            selectedColoringStrategy.getColor(point)
        } else {
            deselectedColoringStrategy.getColor(point)
        }
    }
}