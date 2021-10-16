package com.kylecorry.trail_sense.navigation.infrastructure.persistence

import androidx.annotation.ColorInt
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.database.Identifiable
import com.kylecorry.trail_sense.shared.paths.LineStyle
import com.kylecorry.trail_sense.shared.paths.PathOwner
import com.kylecorry.trail_sense.shared.paths.PathPointColoringStyle
import com.kylecorry.trail_sense.shared.paths.PathStyle

data class PathEntity(
    val name: String?,
    // Style
    val lineStyle: LineStyle,
    val pointStyle: PathPointColoringStyle,
    @ColorInt val color: Int,
    val visible: Boolean,
    // Saved
    val temporary: Boolean = false,
    val owner: PathOwner = PathOwner.User,
    // Metadata
    val distance: Double,
    val numWaypoints: Int,
    // Bounds
    val north: Double,
    val east: Double,
    val south: Double,
    val west: Double,
) : Identifiable {
    override var id: Long = 0L

    val bounds = CoordinateBounds(north, east, south, west)
    val style = PathStyle(lineStyle, pointStyle, color, visible)
}