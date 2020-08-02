package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.trail_sense.shared.domain.Coordinate

data class Beacon(val id: Int, val name: String, val coordinate: Coordinate, val visible: Boolean = true)