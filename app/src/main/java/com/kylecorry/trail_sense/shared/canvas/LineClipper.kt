package com.kylecorry.trail_sense.shared.canvas

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.filters.RDPFilter
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.math.geometry.Line
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.trail_sense.shared.extensions.isSamePixel
import com.kylecorry.trail_sense.shared.extensions.squaredDistanceTo
import com.kylecorry.trail_sense.shared.toPixelCoordinate
import com.kylecorry.trail_sense.shared.toVector2
import kotlin.math.absoluteValue

class LineClipper {

    fun clip(
        pixels: List<PixelCoordinate>,
        bounds: Rectangle,
        output: MutableList<Float>,
        origin: PixelCoordinate = PixelCoordinate(0f, 0f),
        preventLineWrapping: Boolean = false,
        rdpFilterEpsilon: Float? = null
    ) {
        // TODO: Is this allocation needed? What if the bounds were flipped?
        val vectors = pixels.map { it.toVector2(bounds.top) }

        if (isOutOfBounds(vectors, bounds)) {
            return
        }

        val filter =
            if (rdpFilterEpsilon != null) RDPFilter<Int>(rdpFilterEpsilon) { pointIdx, startIdx, endIdx ->
                Geometry.pointLineDistance(
                    vectors[pointIdx],
                    Line(vectors[startIdx], vectors[endIdx])
                ).absoluteValue
            } else null

        val filteredIndices = filter?.filter(pixels.indices.toList()) ?: pixels.indices.toList()

        val multiplier = 1.5f

        val minX = bounds.width() * -multiplier
        val maxX = bounds.width() * (1 + multiplier)
        val minY = bounds.height() * -multiplier
        val maxY = bounds.height() * (1 + multiplier)

        var previous: PixelCoordinate? = null
        var previousVector: Vector2? = null

        for (idx in filteredIndices) {
            val pixel = pixels[idx]
            val vector = vectors[idx]
            // Remove points that are NaN
            if (pixel.x.isNaN() || pixel.y.isNaN()) continue
            // Remove lines that cross the entire screen (because they are behind the camera)
            val isLineInvalid = preventLineWrapping && previous != null &&
                    (pixel.x < minX && previous.x > maxX ||
                            pixel.x > maxX && previous.x < minX ||
                            pixel.y < minY && previous.y > maxY ||
                            pixel.y > maxY && previous.y < minY)

            if (previous != null && previousVector != null && !isLineInvalid) {
                // If the end point is the same as the previous, don't draw a line
                if (previous.isSamePixel(pixel)) {
                    continue
                }
                addLine(
                    bounds,
                    previous,
                    previousVector,
                    pixel,
                    vector,
                    origin,
                    output
                )
            }
            previous = pixel
            previousVector = vector
        }
    }

    private fun isOutOfBounds(pixels: List<Vector2>, bounds: Rectangle): Boolean {
        for (i in 1 until pixels.size) {
            val start = pixels[i - 1]
            val end = pixels[i]
            if (!(start.x < bounds.left && end.x < bounds.left ||
                        start.x > bounds.right && end.x > bounds.right ||
                        start.y < bounds.bottom && end.y < bounds.bottom ||
                        start.y > bounds.top && end.y > bounds.top)
            ) {
                // Potential intersection
                return false
            }
        }

        return true
    }

    private fun addLine(
        bounds: Rectangle,
        start: PixelCoordinate,
        startVector: Vector2,
        end: PixelCoordinate,
        endVector: Vector2,
        origin: PixelCoordinate,
        lines: MutableList<Float>
    ) {
        // Both are in
        if (bounds.contains(startVector) && bounds.contains(endVector)) {
            lines.add(start.x - origin.x)
            lines.add(start.y - origin.y)
            lines.add(end.x - origin.x)
            lines.add(end.y - origin.y)
            return
        }

        val intersection =
            Geometry.getIntersection(startVector, endVector, bounds)
                .map { it.toPixelCoordinate(bounds.top) }

        // A is in, B is not
        if (bounds.contains(startVector)) {
            if (intersection.any()) {
                lines.add(start.x - origin.x)
                lines.add(start.y - origin.y)
                lines.add(intersection[0].x - origin.x)
                lines.add(intersection[0].y - origin.y)
            }
            return
        }

        // B is in, A is not
        if (bounds.contains(endVector)) {
            if (intersection.any()) {
                lines.add(intersection[0].x - origin.x)
                lines.add(intersection[0].y - origin.y)
                lines.add(end.x - origin.x)
                lines.add(end.y - origin.y)
            }
            return
        }

        // Both are out, but may intersect
        if (intersection.size == 2) {
            lines.add(intersection[0].x - origin.x)
            lines.add(intersection[0].y - origin.y)
            lines.add(intersection[1].x - origin.x)
            lines.add(intersection[1].y - origin.y)
        }
    }

}