package com.kylecorry.trail_sense.tools.species_catalog

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class Habitat(override val id: Long) : Identifiable {
    Forest(1),
    Grassland(2),
    Desert(3),
    Wetland(4),
    Freshwater(5),
    Marine(6),
    Tundra(7),
    Mountain(8),
    Urban(9),
    Cave(10)
}