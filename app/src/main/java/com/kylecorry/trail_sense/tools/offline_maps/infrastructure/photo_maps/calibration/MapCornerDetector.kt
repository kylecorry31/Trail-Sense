package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.kylecorry.andromeda.bitmaps.operations.getPixels
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.algebra.forEach
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.math.geometry.Gradients
import com.kylecorry.sol.math.geometry.HoughTransform
import com.kylecorry.sol.math.geometry.LineCandidate
import com.kylecorry.sol.math.geometry.Polygon
import com.kylecorry.sol.math.sumOfFloat
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PixelBounds
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.GradientCalculator
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MapCornerDetector(
    private val maxDimension: Int = 320,
    private val minGradientThreshold: Float = 2.5f,
    private val thetaBins: Int = 128,
    private val minAreaRatio: Float = 0.1f,
    private val maxLineCandidates: Int = 16,
    private val maxCornerCandidates: Int = 32,
    private val minCornerAngleDegrees: Float = 22.5f,
    private val gradientStdDevThreshold: Float = 0.5f
) {

    private val gradientCalculator = GradientCalculator()

    fun detect(bitmap: Bitmap): PixelBounds? {
        if (bitmap.width < IMAGE_MIN_SIZE || bitmap.height < IMAGE_MIN_SIZE) {
            return null
        }

        val scale = Geometry.scaleToFit(
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            maxDimension.toFloat(),
            maxDimension.toFloat()
        )
        val scaled = if (scale < 1f) {
            bitmap.scale(
                max(1, (bitmap.width * scale).roundToInt()),
                max(1, (bitmap.height * scale).roundToInt())
            )
        } else {
            bitmap
        }

        val pixels = scaled.getPixels()
        val result = detect(pixels, scaled.width, scaled.height)

        if (scaled == bitmap) {
            return result
        }

        val xScale = bitmap.width / scaled.width.toFloat()
        val yScale = bitmap.height / scaled.height.toFloat()
        val scaledResult = result?.scaleBounds(xScale, yScale)

        scaled.recycle()
        return scaledResult
    }

    private fun detect(
        pixels: IntArray,
        width: Int,
        height: Int
    ): PixelBounds? {
        val gradients = gradientCalculator.calculate(pixels, width, height)
        val threshold = getThreshold(gradients.magnitude)
        val lines = detectLines(gradients, threshold)
        val corners = getCornerCandidates(lines, width, height)
        val bestCandidate = detectBestQuadrilateral(corners, gradients.magnitude, width, height, threshold)
        return bestCandidate?.polygon?.toPixelBounds()
    }

    private fun Polygon.toPixelBounds(): PixelBounds {
        return PixelBounds(
            vertices[0],
            vertices[1],
            vertices[3],
            vertices[2]
        )
    }

    private fun detectLines(
        gradients: Gradients,
        threshold: Float
    ): List<LineCandidate> {
        return HoughTransform.vote(
            gradients,
            threshold,
            thetaBins
        ).findStrongestLineCandidates(maxLineCandidates)
    }

    private fun detectBestQuadrilateral(
        corners: List<CornerCandidate>,
        gradientMagnitude: Matrix,
        width: Int,
        height: Int,
        threshold: Float
    ): QuadrilateralCandidate? {
        if (corners.size < 4) {
            return null
        }

        var bestCandidate: QuadrilateralCandidate? = null

        val quadrilaterals = getAllQuadrilaterals(corners)

        for (quadrilateral in quadrilaterals) {
            if (!isGoodCandidate(quadrilateral, width, height)) {
                continue
            }

            val score = scoreQuadrilateral(quadrilateral, gradientMagnitude, width, height, threshold)
            if (bestCandidate == null || score > bestCandidate.score) {
                bestCandidate = QuadrilateralCandidate(quadrilateral, score)
            }
        }

        return bestCandidate
    }

    private fun getAllQuadrilaterals(corners: List<CornerCandidate>): List<Polygon> {
        val quadrilaterals = mutableListOf<Polygon>()
        for (i in 0 until corners.size - 3) {
            for (j in i + 1 until corners.size - 2) {
                for (k in j + 1 until corners.size - 1) {
                    for (l in k + 1 until corners.size) {
                        quadrilaterals.add(
                            Polygon.connectCounterClockwise(
                                listOf(
                                    corners[i].point,
                                    corners[j].point,
                                    corners[k].point,
                                    corners[l].point
                                )
                            )
                        )
                    }
                }
            }
        }
        return quadrilaterals
    }

    private fun getCornerCandidates(
        lines: List<LineCandidate>,
        width: Int,
        height: Int
    ): List<CornerCandidate> {
        val candidates = mutableListOf<CornerCandidate>()

        for (i in 0 until lines.size - 1) {
            for (j in i + 1 until lines.size) {
                val line1 = lines[i]
                val line2 = lines[j]
                val intersection = tryGetIntersection(line1, line2, width, height)
                if (intersection != null) {
                    candidates.add(CornerCandidate(intersection, line1.score + line2.score))
                }
            }
        }

        return dedupeCorners(candidates, min(width, height) * CORNER_MERGE_DISTANCE_RATIO)
            .sortedByDescending { it.score }
            .take(maxCornerCandidates)
    }

    private fun tryGetIntersection(line1: LineCandidate, line2: LineCandidate, width: Int, height: Int): Vector2? {
        val angle = line1.line.angleBetween(line2.line).toDegrees()
        if (angle < minCornerAngleDegrees) {
            return null
        }

        val intersection = line1.line.intersect(line2.line)
        return intersection?.takeIf { isNearImage(it, width, height) }
    }

    private fun dedupeCorners(
        corners: List<CornerCandidate>,
        mergeDistance: Float
    ): List<CornerCandidate> {
        val deduped = mutableListOf<CornerCandidate>()

        for (corner in corners.sortedByDescending { it.score }) {
            val hasNearbyCorner = deduped.any { it.point.distanceTo(corner.point) < mergeDistance }
            if (!hasNearbyCorner) {
                deduped.add(corner)
            }
        }

        return deduped
    }

    private fun isNearImage(
        point: PixelCoordinate,
        width: Int,
        height: Int
    ): Boolean {
        val margin = min(width, height) * IMAGE_MARGIN_RATIO
        return point.x in -margin..(width - 1f + margin) && point.y in -margin..(height - 1f + margin)
    }

    private fun getThreshold(magnitude: Matrix): Float {
        val mean = magnitude.sum() / magnitude.size()
        var variance = 0.0
        magnitude.forEach { value ->
            val delta = value - mean
            variance += delta * delta
        }
        variance /= max(1, magnitude.size())
        val stdDev = sqrt(variance).toFloat()
        return max(minGradientThreshold, mean + stdDev * gradientStdDevThreshold)
    }

    private fun isGoodCandidate(polygon: Polygon, width: Int, height: Int): Boolean {
        val minDimension = min(width, height).toFloat()
        val points = polygon.vertices
        val topWidth = points[0].distanceTo(points[1])
        val bottomWidth = points[3].distanceTo(points[2])
        val leftHeight = points[0].distanceTo(points[3])
        val rightHeight = points[1].distanceTo(points[2])

        val hasShortHorizontalEdge =
            topWidth < minDimension * MIN_EDGE_LENGTH_RATIO || bottomWidth < minDimension * MIN_EDGE_LENGTH_RATIO
        val hasShortVerticalEdge =
            leftHeight < minDimension * MIN_EDGE_LENGTH_RATIO || rightHeight < minDimension * MIN_EDGE_LENGTH_RATIO
        val hasShortEdge = hasShortVerticalEdge || hasShortHorizontalEdge

        if (hasShortEdge || !polygon.isConvex() || !hasValidCornerAngles(polygon)) {
            return false
        }

        val area = polygon.area()
        val imageArea = width * height.toFloat()
        return area in (imageArea * minAreaRatio)..(imageArea * 0.999f)
    }

    private fun scoreQuadrilateral(
        polygon: Polygon,
        gradientMagnitude: Matrix,
        width: Int,
        height: Int,
        threshold: Float
    ): Float {
        return polygon.edges.sumOfFloat { scoreEdge(it.start, it.end, gradientMagnitude, width, height, threshold) }
    }

    private fun scoreEdge(
        start: PixelCoordinate,
        end: PixelCoordinate,
        gradientMagnitude: Matrix,
        width: Int,
        height: Int,
        threshold: Float
    ): Float {
        val length = start.distanceTo(end)
        val steps = max(1, length.roundToInt())
        var score = 0f

        for (step in 0..steps) {
            val t = step / steps.toFloat()
            val x = start.x + (end.x - start.x) * t
            val y = start.y + (end.y - start.y) * t
            score += sampleEdgeStrength(gradientMagnitude, width, height, x, y, threshold)
        }

        return score / (steps + 1) + SCORE_LENGTH_WEIGHT * length
    }

    private fun sampleEdgeStrength(
        gradientMagnitude: Matrix,
        width: Int,
        height: Int,
        x: Float,
        y: Float,
        threshold: Float
    ): Float {
        val actualX = x.roundToInt().coerceIn(0, width - 1)
        val actualY = y.roundToInt().coerceIn(0, height - 1)
        return (gradientMagnitude[actualY, actualX] - threshold) / threshold.coerceIn(0f, 1f)
    }

    private fun hasValidCornerAngles(polygon: Polygon): Boolean {
        val points = polygon.vertices
        return points.indices.all { i ->
            val previous = points[(i - 1 + points.size) % points.size]
            val current = points[i]
            val next = points[(i + 1) % points.size]
            val angle = Geometry.getInteriorAngle(previous, current, next)
            angle in minCornerAngleDegrees..(180f - minCornerAngleDegrees)
        }
    }

    private data class CornerCandidate(
        val point: PixelCoordinate,
        val score: Float
    )

    private data class QuadrilateralCandidate(
        val polygon: Polygon,
        val score: Float
    )

    private fun PixelBounds.scaleBounds(xScale: Float, yScale: Float): PixelBounds {
        return PixelBounds(
            com.kylecorry.andromeda.core.units.PixelCoordinate(
                topLeft.x * xScale,
                topLeft.y * yScale
            ),
            com.kylecorry.andromeda.core.units.PixelCoordinate(
                topRight.x * xScale,
                topRight.y * yScale
            ),
            com.kylecorry.andromeda.core.units.PixelCoordinate(
                bottomLeft.x * xScale,
                bottomLeft.y * yScale
            ),
            com.kylecorry.andromeda.core.units.PixelCoordinate(
                bottomRight.x * xScale,
                bottomRight.y * yScale
            )
        )
    }

    companion object {
        private const val CORNER_MERGE_DISTANCE_RATIO = 0.05f
        private const val IMAGE_MARGIN_RATIO = 0.1f
        private const val IMAGE_MIN_SIZE = 10
        private const val MIN_EDGE_LENGTH_RATIO = 0.3f
        private const val SCORE_LENGTH_WEIGHT = 0.001f
    }
}
