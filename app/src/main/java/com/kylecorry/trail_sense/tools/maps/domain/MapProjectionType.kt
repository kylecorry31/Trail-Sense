package com.kylecorry.trail_sense.tools.maps.domain

import com.kylecorry.trail_sense.shared.database.Identifiable

enum class MapProjectionType(override val id: Long) : Identifiable {
    Mercator(1),
    CylindricalEquidistant(2)
}