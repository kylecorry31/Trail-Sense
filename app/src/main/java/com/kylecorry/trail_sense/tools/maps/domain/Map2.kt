package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.trail_sense.shared.database.Identifiable

data class Map2(
    override val id: Long,
    val name: String,
    val filename: String,
    val calibration: MapCalibration,
    val metadata: MapMetadata
) : Identifiable