package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import com.kylecorry.trail_sense.shared.database.Identifiable
import java.time.Instant

data class TideTableRowEntity(
    override val id: Long,
    val tableId: Long,
    val time: Instant,
    val isHigh: Boolean,
    val heightMeters: Float?
) : Identifiable
