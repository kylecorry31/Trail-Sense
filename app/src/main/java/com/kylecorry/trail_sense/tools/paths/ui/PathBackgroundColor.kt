package com.kylecorry.trail_sense.tools.paths.ui

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class PathBackgroundColor(override val id: Long) : Identifiable {
    None(1),
    Black(2),
    White(3)
}