package com.kylecorry.trail_sense.navigation.paths.ui.drawing

import android.graphics.Color
import android.graphics.Path
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.paths.domain.LineStyle

data class RenderedPath(
    val origin: Coordinate,
    val path: Path,
    val style: LineStyle = LineStyle.Solid,
    val color: Int = Color.BLACK
)