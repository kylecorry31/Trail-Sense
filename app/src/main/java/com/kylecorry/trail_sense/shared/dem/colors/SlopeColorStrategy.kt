package com.kylecorry.trail_sense.shared.dem.colors

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class SlopeColorStrategy(override val id: Long): Identifiable {
    WhiteToRed(1),
    Grayscale(2),
    GreenToRed(3),
}
