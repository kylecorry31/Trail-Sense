package com.kylecorry.trail_sense.tools.map.domain

import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.data.Identifiable
import java.time.Instant

data class OfflineMapFile(
    override val id: Long,
    val name: String,
    val type: OfflineMapFileType,
    val path: String,
    val sizeBytes: Long,
    val createdOn: Instant,
    val bounds: CoordinateBounds?,
    val visible: Boolean
) : Identifiable
