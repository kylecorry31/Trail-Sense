package com.kylecorry.trail_sense.shared

import androidx.annotation.ColorInt
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import java.time.Instant

data class Path(val id: Long, val name: String, val points: List<PathPoint>, @ColorInt val color: Int, val dotted: Boolean = false)
data class PathPoint(val coordinate: Coordinate, val time: Instant? = null)