package com.kylecorry.trail_sense.tools.photo_maps.domain

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class MapProjectionType(override val id: Long) : Identifiable {
    Mercator(1),
    CylindricalEquidistant(2)
}