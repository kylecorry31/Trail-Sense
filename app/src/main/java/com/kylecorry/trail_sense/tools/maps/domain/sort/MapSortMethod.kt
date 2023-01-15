package com.kylecorry.trail_sense.tools.maps.domain.sort

import com.kylecorry.trail_sense.shared.database.Identifiable

enum class MapSortMethod(override val id: Long): Identifiable {
    Closest(1),
    MostRecent(2),
    Name(3)
}