package com.kylecorry.trail_sense.tools.paths.ui.drawing

import android.graphics.Color
import android.graphics.Path
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

data class RenderedPath(
    val origin: Coordinate,
    val line: MutableList<Float>,
    val path: Path? = null,
    val style: LineStyle = LineStyle.Solid,
    val color: Int = Color.BLACK,
    val renderedScale: Float = 1f
)