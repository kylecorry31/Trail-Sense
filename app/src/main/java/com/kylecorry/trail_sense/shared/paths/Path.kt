package com.kylecorry.trail_sense.shared.paths

import androidx.annotation.ColorInt
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.database.Identifiable
import java.time.Instant

data class Path(
    val id: Long,
    val name: String,
    val points: List<PathPoint>,
    @ColorInt val color: Int,
    val style: LineStyle = LineStyle.Solid,
    val temporary: Boolean = false,
    val owner: PathOwner = PathOwner.User
)

data class Path2(
    override val id: Long,
    val name: String?,
    val style: PathStyle,
    val metadata: PathMetadata,
    val temporary: Boolean = false
) : Identifiable

data class PathMetadata(
    val distance: Distance,
    val waypoints: Int,
    val duration: Range<Instant>?,
    val bounds: CoordinateBounds
)