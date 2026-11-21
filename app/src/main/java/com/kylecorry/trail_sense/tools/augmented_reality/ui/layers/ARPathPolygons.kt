package com.kylecorry.trail_sense.tools.augmented_reality.ui.layers

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.kylecorry.sol.math.Vector3
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.AugmentedRealityCoordinate
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARLine
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class ARPathPolygons(private val widthMeters: Float) {

    fun ribbon(
        line: ARLine,
        view: AugmentedRealityView,
        onFocus: (() -> Boolean)? = null
    ): ARPolygon {
        val coordinates = line.points.map { it.getAugmentedRealityCoordinate(view) }
        val positions = coordinates.map { it.position }

        val edges = positions.indices.map { i ->
            val offset = sideOffset(positions, i, widthMeters / 2f)
            (positions[i] + offset) to (positions[i] - offset)
        }

        val quads = (0 until edges.size - 1).map { i ->
            val (left, right) = edges[i]
            val (nextLeft, nextRight) = edges[i + 1]
            listOf(left, nextLeft, nextRight, right).map {
                AugmentedRealityCoordinate(it, coordinates[i].isTrueNorth)
            }
        }
        return ARPolygon(quads, ColorUtils.setAlphaComponent(line.color, RIBBON_ALPHA), onFocus)
    }

    fun arrows(line: ARLine, view: AugmentedRealityView): ARPolygon {
        val coordinates = line.points.map { it.getAugmentedRealityCoordinate(view) }
        val arrows = (0 until coordinates.size - 1).mapNotNull { i ->
            arrow(coordinates[i].position, coordinates[i + 1].position)?.map {
                AugmentedRealityCoordinate(it, coordinates[i].isTrueNorth)
            }
        }
        return ARPolygon(arrows, Color.WHITE)
    }

    fun wallArrows(line: ARLine, view: AugmentedRealityView): List<ARPolygon> {
        val coordinates = line.points.map { it.getAugmentedRealityCoordinate(view) }
        val positions = coordinates.map { it.position }
        val arrows = mutableListOf<List<AugmentedRealityCoordinate>>()
        for (side in listOf(outerSide(positions))) {
            for (i in 0 until positions.size - 1 step WALL_ARROW_EVERY) {
                val start = positions[i] + sideOffset(positions, i, WALL_OFFSET * side)
                val end = positions[i + 1] + sideOffset(positions, i + 1, WALL_OFFSET * side)
                wallChevron(start, end, side)?.let { chevron ->
                    arrows.add(chevron.map { AugmentedRealityCoordinate(it, coordinates[i].isTrueNorth) })
                }
            }
        }
        return listOf(ARPolygon(arrows, CHEVRON_COLOR))
    }

    // The side of the path away from the inside of its bend (+1 is left, -1 is right).
    // Straight paths use the right.
    private fun outerSide(positions: List<Vector3>): Float {
        if (positions.size < 5) return -1f
        // Wide baselines, since the nearest point can be duplicated and give a zero-length segment
        val first = positions[2] - positions[0]
        val last = positions.last() - positions[positions.lastIndex - 2]
        val cross = first.x * last.y - first.y * last.x
        val dot = first.x * last.x + first.y * last.y
        return if (atan2(cross, dot) < -STRAIGHT_THRESHOLD) 1f else -1f
    }

    // A bold open chevron standing upright at the device's height, pointing along the path. It is
    // yawed a little toward the path so the near ones read almost face-on, like a road sign.
    private fun wallChevron(start: Vector3, end: Vector3, side: Float): List<Vector3>? {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = hypot(dx, dy)
        if (length < 0.0001f) return null
        val forward = Vector3(dx / length, dy / length, 0f)
        val inward = Vector3(forward.y * side, -forward.x * side, 0f)
        val dir = forward * cos(WALL_ARROW_YAW) + inward * sin(WALL_ARROW_YAW)
        val mid = (start + end) * 0.5f
        val center = Vector3(mid.x, mid.y, 0f)
        val half = WALL_ARROW_LENGTH / 2f
        val halfHeight = WALL_ARROW_HEIGHT / 2f
        val thickness = WALL_ARROW_LENGTH * 0.45f
        fun point(u: Float, z: Float) = center + dir * u + Vector3(0f, 0f, z)
        return listOf(
            point(-half, halfHeight),
            point(-half + thickness, halfHeight),
            point(half, 0f),
            point(-half + thickness, -halfHeight),
            point(-half, -halfHeight),
            point(0f, 0f)
        )
    }

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

        const val WALL_OFFSET = 2f // meters from the path center, leaves a clear lane
        const val STRAIGHT_THRESHOLD = 0.17f // radians
        const val WALL_ARROW_EVERY = 2 // segments
        const val WALL_ARROW_LENGTH = 1.2f // meters
        const val WALL_ARROW_HEIGHT = 1.4f // meters
        const val WALL_ARROW_YAW = 0.35f // radians toward the path
        val CHEVRON_COLOR = Color.rgb(170, 240, 255)
        const val ARROW_RIBBON_WIDTH_RATIO = 0.8f

        const val ARROW_WIDTH_TO_LENGTH = 0.8f
        const val ARROW_NOTCH_DEPTH = 0.2f
    }
}
