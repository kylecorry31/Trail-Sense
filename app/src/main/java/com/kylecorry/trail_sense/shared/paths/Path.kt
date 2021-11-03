package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.database.Identifiable
import java.time.Instant

data class Path(
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
) {
    companion object {
        val empty = PathMetadata(Distance.meters(0f), 0, null, CoordinateBounds.empty)
    }
}