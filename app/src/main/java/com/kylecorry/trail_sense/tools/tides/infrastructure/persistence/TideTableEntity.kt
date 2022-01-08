package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import com.kylecorry.trail_sense.shared.database.Identifiable

data class TideTableEntity(
    override val id: Long,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?
) : Identifiable
