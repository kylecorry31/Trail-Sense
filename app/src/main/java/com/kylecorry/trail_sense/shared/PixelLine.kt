package com.kylecorry.trail_sense.shared

import androidx.annotation.ColorInt
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.Path
import com.kylecorry.trailsensecore.domain.pixels.PixelCoordinate
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

data class PixelLine(
    val start: PixelCoordinate,
    val end: PixelCoordinate,
    @ColorInt val color: Int,
    val alpha: Int,
    val dotted: Boolean
)

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
            dotted
        )
        lines.add(line)
    }
    return lines
}