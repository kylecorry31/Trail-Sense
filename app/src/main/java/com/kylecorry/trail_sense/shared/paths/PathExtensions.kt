package com.kylecorry.trail_sense.shared.paths

import android.content.Context
import android.graphics.Color
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.navigation.ui.MappableLocation
import com.kylecorry.trail_sense.navigation.ui.MappablePath
import com.kylecorry.trail_sense.shared.canvas.PixelLine
import com.kylecorry.trail_sense.shared.canvas.PixelLineStyle
import com.kylecorry.trail_sense.tools.backtrack.domain.factories.*

fun List<PathPoint>.asMappable(context: Context, path: Path): IMappablePath {
    val colorFactory = getPointFactory(context, path.style.point)
    val strategy = colorFactory.createColoringStrategy(this)
    return MappablePath(path.id, this.map { point ->
        MappableLocation(
            point.id,
            point.coordinate,
            strategy.getColor(point) ?: Color.TRANSPARENT
        )
    }, path.style.color, path.style.line)
}

// TODO: Extract this and add solid style
private fun getPointFactory(
    context: Context,
    pointColoringStyle: PathPointColoringStyle
): IPointDisplayFactory {
    return when (pointColoringStyle) {
        PathPointColoringStyle.None -> NonePointDisplayFactory(context)
        PathPointColoringStyle.CellSignal -> CellSignalPointDisplayFactory(context)
        PathPointColoringStyle.Altitude -> AltitudePointDisplayFactory(context)
        PathPointColoringStyle.Time -> TimePointDisplayFactory(context)
    }
}

fun IMappablePath.toPixelLines(
    toPixelCoordinate: (coordinate: Coordinate) -> PixelCoordinate
): List<PixelLine> {
    val lines = mutableListOf<PixelLine>()
    val pixelWaypoints = points.map {
        toPixelCoordinate(it.coordinate)
    }
    for (i in 1 until pixelWaypoints.size) {
        val line = PixelLine(
            pixelWaypoints[i - 1],
            pixelWaypoints[i],
            color,
            255,
            mapPixelLineStyle(style)
        )
        lines.add(line)
    }
    return lines
}

private fun mapPixelLineStyle(style: LineStyle): PixelLineStyle {
    return when (style) {
        LineStyle.Solid -> PixelLineStyle.Solid
        LineStyle.Dotted -> PixelLineStyle.Dotted
        LineStyle.Arrow -> PixelLineStyle.Arrow
    }
}