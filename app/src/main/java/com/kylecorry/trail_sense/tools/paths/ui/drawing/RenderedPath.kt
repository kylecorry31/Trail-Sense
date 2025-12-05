package com.kylecorry.trail_sense.tools.paths.ui.drawing

import android.graphics.Color
import android.graphics.Path
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

data class RenderedPath(
    val origin: Coordinate,
    val line: FloatArray,
    val path: Path? = null,
    val style: LineStyle = LineStyle.Solid,
    val color: Int = Color.BLACK,
    val renderedScale: Float = 1f,
    val originalFeature: GeoJsonFeature? = null,
)