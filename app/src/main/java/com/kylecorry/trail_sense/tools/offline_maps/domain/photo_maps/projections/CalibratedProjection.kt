package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate

class CalibratedProjection(
    calibration: List<Pair<PixelCoordinate, Coordinate>>,
    private val projection: IMapProjection
) : IMapProjection {

    private val first = calibration.getOrNull(0).takeIf { calibration.size == 2 }
    private val second = calibration.getOrNull(1).takeIf { calibration.size == 2 }
    private val imageAnchor = first?.let {
        toMathCoordinates(Vector2(it.first.x, it.first.y))
    } ?: Vector2.zero
    private val projectedAnchor = first?.let {
        projection.toPixels(it.second)
    } ?: Vector2.zero
    private val imageVector = second?.let {
        toMathCoordinates(Vector2(it.first.x, it.first.y)) - imageAnchor
    } ?: Vector2.zero
    private val projectedVector = second?.let {
        projection.toPixels(it.second) - projectedAnchor
    } ?: Vector2.zero
    private val imageVectorMagnitudeSquared = imageVector.squaredMagnitude()
    private val scaledCos = if (imageVectorMagnitudeSquared == 0f) {
        0f
    } else {
        dot(projectedVector, imageVector) / imageVectorMagnitudeSquared
    }
    private val scaledSin = if (imageVectorMagnitudeSquared == 0f) {
        0f
    } else {
        cross(imageVector, projectedVector) / imageVectorMagnitudeSquared
    }
    private val transformMagnitudeSquared = scaledCos * scaledCos + scaledSin * scaledSin

    override fun toCoordinate(pixel: Vector2): Coordinate {
        if (imageVectorMagnitudeSquared == 0f) {
            return Coordinate.zero
        }

        val imageDelta = toMathCoordinates(pixel) - imageAnchor
        val projectedDelta = applyImageToProjectedMatrix(imageDelta)

        return projection.toCoordinate(projectedAnchor + projectedDelta)
    }

    override fun toPixels(location: Coordinate): Vector2 {
        return toPixels(location.latitude, location.longitude)
    }

    override fun toPixels(
        latitude: Double,
        longitude: Double
    ): Vector2 {
        if (transformMagnitudeSquared == 0f) {
            return Vector2.zero
        }

        val projectedDelta = projection.toPixels(latitude, longitude) - projectedAnchor
        val imageDelta = applyProjectedToImageMatrix(projectedDelta)

        return toImageCoordinates(imageAnchor + imageDelta)
    }

    private fun applyImageToProjectedMatrix(imageDelta: Vector2): Vector2 {
        return Vector2(
            scaledCos * imageDelta.x - scaledSin * imageDelta.y,
            scaledSin * imageDelta.x + scaledCos * imageDelta.y
        )
    }

    private fun applyProjectedToImageMatrix(projectedDelta: Vector2): Vector2 {
        return Vector2(
            (scaledCos * projectedDelta.x + scaledSin * projectedDelta.y) / transformMagnitudeSquared,
            (-scaledSin * projectedDelta.x + scaledCos * projectedDelta.y) / transformMagnitudeSquared
        )
    }

    private fun toMathCoordinates(pixel: Vector2): Vector2 {
        return Vector2(pixel.x, -pixel.y)
    }

    private fun toImageCoordinates(pixel: Vector2): Vector2 {
        return Vector2(pixel.x, -pixel.y)
    }

    private fun dot(a: Vector2, b: Vector2): Float {
        return a.x * b.x + a.y * b.y
    }

    private fun cross(a: Vector2, b: Vector2): Float {
        return a.x * b.y - a.y * b.x
    }

}
