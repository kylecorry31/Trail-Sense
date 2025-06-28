package com.kylecorry.trail_sense.tools.photo_maps.domain.sort

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class MapSortMethod(override val id: Long): Identifiable {
    Closest(1),
    MostRecent(2),
    Name(3)
}