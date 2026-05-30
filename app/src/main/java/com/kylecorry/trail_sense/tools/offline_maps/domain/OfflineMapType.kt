package com.kylecorry.trail_sense.tools.offline_maps.domain

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class OfflineMapType(override val id: Long): Identifiable {
    Photo(1),
    Trail(2)
}
