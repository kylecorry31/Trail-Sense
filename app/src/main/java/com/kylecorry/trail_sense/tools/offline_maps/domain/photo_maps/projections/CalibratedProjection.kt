package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.andromeda_temp.cross
import com.kylecorry.trail_sense.shared.andromeda_temp.dot
import kotlin.math.abs

/**
 * Performs a 2 point calibration of a projection
 */
class CalibratedProjection(
    calibration: List<Pair<PixelCoordinate, Coordinate>>,
    private val projection: IMapProjection
) : IMapProjection {

    private val sortedCalibration = calibration
        .takeIf { it.size == 2 }
        ?.sortedWith(compareBy({ it.first.x }, { it.first.y }))
        ?: emptyList()
    private val first = sortedCalibration.getOrNull(0)
    private val second = sortedCalibration.getOrNull(1)
    private val crossesAntimeridian = first != null && second != null && crossesAntimeridian(
        first.second.longitude,
        second.second.longitude
    )
    private val imageAnchor = first?.let {
        toMathCoordinates(Vector2(it.first.x, it.first.y))
    } ?: Vector2.zero
    private val projectedAnchor = first?.let {
        projection.toPixels(it.second)
    } ?: Vector2.zero
    private val projectedWorldWidth = if (crossesAntimeridian) {
        estimateProjectedWorldWidth()
    } else {
        0f
    }
    private val imageVector = second?.let {
        toMathCoordinates(Vector2(it.first.x, it.first.y)) - imageAnchor
    } ?: Vector2.zero
    private val projectedVector = second?.let {
        normalizeProjectedPixel(projection.toPixels(it.second)) - projectedAnchor
    } ?: Vector2.zero
    private val imageVectorMagnitudeSquared = imageVector.squaredMagnitude()
    private val hasValidImageVector = !Arithmetic.isZero(imageVectorMagnitudeSquared)
    private val scaledCos = if (Arithmetic.isZero(imageVectorMagnitudeSquared)) {
        0f
    } else {
        projectedVector.dot(imageVector) / imageVectorMagnitudeSquared
    }
    private val scaledSin = if (Arithmetic.isZero(imageVectorMagnitudeSquared)) {
        0f
    } else {
        imageVector.cross(projectedVector) / imageVectorMagnitudeSquared
    }
    private val transformMagnitudeSquared = scaledCos * scaledCos + scaledSin * scaledSin
    private val hasValidTransform = !Arithmetic.isZero(
        transformMagnitudeSquared,
        Arithmetic.EPSILON_FLOAT * Arithmetic.EPSILON_FLOAT
    )

    /*
     * Equivalent to:
     * matrix.postTranslate(-imageAnchor.x, -imageAnchor.y)
     * matrix.postScale(scale, scale)
     * matrix.postRotate(rotationDegrees)
     * matrix.postTranslate(projectedAnchor.x, projectedAnchor.y)
     */
    private val imageToProjectedMatrix = Matrix.create(
        3,
        3,
        floatArrayOf(
            scaledCos,
            -scaledSin,
            projectedAnchor.x - scaledCos * imageAnchor.x + scaledSin * imageAnchor.y,
            scaledSin,
            scaledCos,
            projectedAnchor.y - scaledSin * imageAnchor.x - scaledCos * imageAnchor.y,
            0f,
            0f,
            1f
        )
    )
    private val projectedToImageMatrix = if (hasValidTransform) {
        val inverseScaledCos = scaledCos / transformMagnitudeSquared
        val inverseScaledSin = -scaledSin / transformMagnitudeSquared
        Matrix.create(
            3,
            3,
            floatArrayOf(
                inverseScaledCos,
                -inverseScaledSin,
                imageAnchor.x - inverseScaledCos * projectedAnchor.x + inverseScaledSin * projectedAnchor.y,
                inverseScaledSin,
                inverseScaledCos,
                imageAnchor.y - inverseScaledSin * projectedAnchor.x - inverseScaledCos * projectedAnchor.y,
                0f,
                0f,
                1f
            )
        )
    } else {
        Matrix.zeros(3, 3)
    }

    override fun toCoordinate(pixel: Vector2): Coordinate {
        if (!hasValidImageVector) {
            return Coordinate.zero
        }

        val projectedPixel = imageToProjectedMatrix.dot(toMathCoordinates(pixel))
        val coordinate = projection.toCoordinate(projectedPixel)
        return if (crossesAntimeridian) {
            Coordinate(coordinate.latitude, Coordinate.toLongitude(coordinate.longitude))
        } else {
            coordinate
        }
    }

    override fun toPixels(location: Coordinate): Vector2 {
        return toPixels(location.latitude, location.longitude)
    }

    override fun toPixels(
        latitude: Double,
        longitude: Double
    ): Vector2 {
        if (!hasValidTransform) {
            return Vector2.zero
        }

        val imagePixel = projectedToImageMatrix.dot(
            normalizeProjectedPixel(projection.toPixels(latitude, longitude))
        )
        return toImageCoordinates(imagePixel)
    }

    private fun normalizeProjectedPixel(pixel: Vector2): Vector2 {
        if (!crossesAntimeridian || Arithmetic.isZero(projectedWorldWidth)) {
            return pixel
        }

        val delta = pixel.x - projectedAnchor.x
        val normalizedX = when {
            delta < -projectedWorldWidth / 2f -> pixel.x + projectedWorldWidth
            delta > projectedWorldWidth / 2f -> pixel.x - projectedWorldWidth
            else -> pixel.x
        }

        return Vector2(normalizedX, pixel.y)
    }

    private fun crossesAntimeridian(firstLongitude: Double, secondLongitude: Double): Boolean {
        val delta = abs(secondLongitude - firstLongitude)
        return delta > 180.0 && delta < 360.0
    }

    private fun estimateProjectedWorldWidth(): Float {
        val zeroLongitude = projection.toPixels(0.0, 0.0).x
        val oneDegreeLongitude = projection.toPixels(0.0, 1.0).x
        return abs(oneDegreeLongitude - zeroLongitude) * 360f
    }

    // toMathCoordinate and toImageCoordinate intentionally do the same thing, it's just clearer to describe what they are doing
    private fun toMathCoordinates(pixel: Vector2): Vector2 {
        return Vector2(pixel.x, -pixel.y)
    }

    private fun toImageCoordinates(pixel: Vector2): Vector2 {
        return Vector2(pixel.x, -pixel.y)
    }

}
