package com.kylecorry.trail_sense.shared.dem.colors

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class ElevationColorStrategy(override val id: Long) : Identifiable {
    Brown(1),
    White(2),
    Black(3),
    Gray(4),
    USGS(5),
    Grayscale(6),
    Vibrant(7),
    Muted(8),
    Viridis(9),
    Inferno(10),
    Plasma(11),
}