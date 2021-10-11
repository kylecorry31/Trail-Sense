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
import com.kylecorry.trail_sense.tools.backtrack.domain.factories.TimePointDisplayFactory
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

fun Path.toPixelLines(
    fadeDuration: Duration,
    toPixelCoordinate: (coordinate: Coordinate) -> PixelCoordinate
): List<PixelLine> {
    val lines = mutableListOf<PixelLine>()
    val pixelWaypoints = points.map {
        Pair(toPixelCoordinate(it.coordinate), it.time)
    }
    val now = Instant.now().toEpochMilli()
    for (i in 1 until pixelWaypoints.size) {
        val hasTime = pixelWaypoints[i - 1].second != null
        val timeAgo =
            if (hasTime) abs(now - pixelWaypoints[i - 1].second!!.toEpochMilli()) / 1000f else 0f
        if (hasTime && timeAgo >= fadeDuration.seconds) {
            continue
        }

        val line = PixelLine(
            pixelWaypoints[i - 1].first,
            pixelWaypoints[i].first,
            color,
            if (!hasTime) 255 else (255 * (1 - timeAgo / fadeDuration.seconds)).toInt()
                .coerceIn(60, 255),
            mapPixelLineStyle(style)
        )
        lines.add(line)
    }
    return lines
}

fun Path.asMappable(context: Context): IMappablePath {
    val colorFactory = TimePointDisplayFactory(context)
    val strategy = colorFactory.createColoringStrategy(points)
    return MappablePath(id, points.map { point ->
        MappableLocation(
            point.id,
            point.coordinate,
            strategy.getColor(point) ?: Color.TRANSPARENT
        )
    }, color, style)
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