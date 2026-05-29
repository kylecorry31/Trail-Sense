package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.kylecorry.andromeda.bitmaps.operations.getPixels
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.specifications.Specification
import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.algebra.forEach
import com.kylecorry.sol.math.geometry.Geometry
import com.kylecorry.sol.math.geometry.Gradients
import com.kylecorry.sol.math.geometry.HoughTransform
import com.kylecorry.sol.math.geometry.LineCandidate
import com.kylecorry.sol.math.geometry.Polygon
import com.kylecorry.trail_sense.shared.andromeda_temp.combinations
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PixelBounds
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.GradientCalculator
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.scoring.AggregateQuadrilateralScoringStrategy
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.scoring.EdgeMagnitudeQuadrilateralScoringStrategy
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.scoring.PerimeterQuadrilateralScoringStrategy
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.scoring.QuadrilateralScoringStrategy
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.selection.HasValidAreaSpecification
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.selection.HasValidCornerAnglesSpecification
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.selection.HasValidEdgeLengthsSpecification
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.selection.IsConvexSpecification
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.selection.QuadrilateralSelectionCriteria
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MapCornerDetector(
    private val maxDimension: Int = 320,
    private val minGradientThreshold: Float = 2.5f,
    private val thetaBins: Int = 128,
    private val maxLineCandidates: Int = 16,
    private val maxCornerCandidates: Int = 32,
    private val minCornerAngleDegrees: Float = 22.5f,
    private val gradientStdDevThreshold: Float = 0.5f,
    private val scoringStrategy: QuadrilateralScoringStrategy = AggregateQuadrilateralScoringStrategy(
        listOf(
            EdgeMagnitudeQuadrilateralScoringStrategy() to 1f,
            PerimeterQuadrilateralScoringStrategy() to 0.001f
        )
    ),
    private val selectionSpecification: Specification<QuadrilateralSelectionCriteria> =
        HasValidEdgeLengthsSpecification(0.3f)
            .and(IsConvexSpecification())
            .and(HasValidCornerAnglesSpecification(minCornerAngleDegrees))
            .and(HasValidAreaSpecification(Range(0.1f, 0.999f)))
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
        val bestCandidate = detectBestQuadrilateral(corners, gradients, width, height, threshold)
        return bestCandidate?.polygon?.toPixelBounds()
    }

    private fun Polygon.toPixelBounds(): PixelBounds {
        val topLeftIndex = vertices.indices.minByOrNull {
            vertices[it].x + vertices[it].y
        } ?: 0
        val reoriented = vertices.drop(topLeftIndex) + vertices.take(topLeftIndex)
        return PixelBounds(
            reoriented[0],
            reoriented[1],
            reoriented[3],
            reoriented[2]
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
        gradients: Gradients,
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
            if (!selectionSpecification.isSatisfiedBy(QuadrilateralSelectionCriteria(quadrilateral, width, height))) {
                continue
            }

            val score = scoringStrategy.score(quadrilateral, gradients, threshold)
            if (bestCandidate == null || score > bestCandidate.score) {
                bestCandidate = QuadrilateralCandidate(quadrilateral, score)
            }
        }

        return bestCandidate
    }

    private fun getAllQuadrilaterals(corners: List<CornerCandidate>): List<Polygon> {
        return corners
            .combinations(4)
            .map { combination ->
                Polygon.connectCounterClockwise(combination.map { it.point })
            }
    }

    private fun getCornerCandidates(
        lines: List<LineCandidate>,
        width: Int,
        height: Int
    ): List<CornerCandidate> {
        val candidates = lines
            .combinations(2)
            .mapNotNull { (line1, line2) ->
                val intersection = tryGetIntersection(line1, line2, width, height)
                intersection?.let {
                    CornerCandidate(it, line1.score + line2.score)
                }
            }

        return deduplicateCorners(candidates, min(width, height) * CORNER_MERGE_DISTANCE_RATIO)
            .sortedByDescending { it.score }
            .take(maxCornerCandidates)
    }

    private fun tryGetIntersection(line1: LineCandidate, line2: LineCandidate, width: Int, height: Int): Vector2? {
        val angle = line1.line.angleBetween(line2.line).toDegrees()
        if (angle < minCornerAngleDegrees) {
            return null
        }

        val intersection = line1.line.intersect(line2.line)
        return intersection?.takeIf { isWithinImageMargin(it, width, height) }
    }

    private fun deduplicateCorners(
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

    private fun isWithinImageMargin(
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
    }
}
