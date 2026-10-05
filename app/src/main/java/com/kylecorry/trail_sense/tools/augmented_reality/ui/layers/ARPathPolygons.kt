package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.kylecorry.sol.math.Vector3
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARLine
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import kotlin.math.hypot

/**
 * Builds the polygons used to draw a path on the ground
 */
class ARPathPolygons(private val widthMeters: Float) {

    /** A semi-transparent ribbon centered on the line, in the line's color */
    fun ribbon(
        line: ARLine,
        view: AugmentedRealityView,
        onFocus: (() -> Boolean)? = null
    ): ARPolygon {
        val coordinates = line.points.map { it.getAugmentedRealityCoordinate(view) }
        val positions = coordinates.map { it.position }

        // Left and right edge of the ribbon at each point
        val edges = positions.indices.map { i ->
            val offset = sideOffset(positions, i, widthMeters / 2f)
            (positions[i] + offset) to (positions[i] - offset)
        }

        // One quad per segment
        val quads = (0 until edges.size - 1).map { i ->
            val (left, right) = edges[i]
            val (nextLeft, nextRight) = edges[i + 1]
            listOf(left, nextLeft, nextRight, right).map {
                AugmentedRealityCoordinate(it, coordinates[i].isTrueNorth)
            }
        }
        return ARPolygon(quads, ColorUtils.setAlphaComponent(line.color, RIBBON_ALPHA), onFocus)
    }

    /** White arrowheads on the ribbon pointing toward the end of the line */
    fun arrows(line: ARLine, view: AugmentedRealityView): ARPolygon {
        val coordinates = line.points.map { it.getAugmentedRealityCoordinate(view) }
        val arrows = (0 until coordinates.size - 1).mapNotNull { i ->
            arrow(coordinates[i].position, coordinates[i + 1].position)?.map {
                AugmentedRealityCoordinate(it, coordinates[i].isTrueNorth)
            }
        }
        return ARPolygon(arrows, Color.WHITE)
    }

    /** An arrowhead centered on the segment, or null if the segment is too short to fit one */
    private fun arrow(start: Vector3, end: Vector3): List<Vector3>? {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = hypot(dx, dy)
        val arrowWidth = widthMeters * ARROW_RIBBON_WIDTH_RATIO
        val arrowLength = arrowWidth / ARROW_WIDTH_TO_LENGTH
        if (length < arrowLength * 1.5f) return null
        val forward = Vector3(dx / length, dy / length, 0f)
        val side = Vector3(-forward.y, forward.x, 0f)
        val center = (start + end) * 0.5f
        val halfLength = arrowLength / 2f
        val halfWidth = arrowWidth / 2f
        return listOf(
            center + forward * halfLength,
            center - forward * halfLength + side * halfWidth,
            center - forward * (halfLength - arrowLength * ARROW_NOTCH_DEPTH),
            center - forward * halfLength - side * halfWidth
        )
    }

    /**
     * The horizontal vector perpendicular to the path at a point. The direction is taken from the
     * neighboring points so adjacent segments join without gaps.
     */
    private fun sideOffset(positions: List<Vector3>, index: Int, length: Float): Vector3 {
        val prev = positions[maxOf(index - 1, 0)]
        val next = positions[minOf(index + 1, positions.lastIndex)]
        val dx = next.x - prev.x
        val dy = next.y - prev.y
        val norm = hypot(dx, dy)
        if (norm < 0.0001f) return Vector3.zero
        return Vector3(-dy / norm * length, dx / norm * length, 0f)
    }

    private companion object {
        const val RIBBON_ALPHA = 200
        const val ARROW_RIBBON_WIDTH_RATIO = 0.8f

        // Same proportions as the arrow on the my location map layer
        const val ARROW_WIDTH_TO_LENGTH = 0.8f
        const val ARROW_NOTCH_DEPTH = 0.2f
    }
}
