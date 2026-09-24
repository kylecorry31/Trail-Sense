package com.kylecorry.trail_sense.tools.augmented_reality.domain.position

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * A point in the AR world
 * @param location The location of the point
 * @param elevation The elevation of the point in meters, defaults to the elevation of the camera
 * @param actualDiameter The actual diameter of the point in meters, defaults to 1 meter
 */
class GeographicARPoint(
    val location: Coordinate,
    val elevation: Float? = null,
    val actualDiameter: Float = 1f,
    val isElevationRelative: Boolean = false
) : ARPoint {
    private data class CalculatedPoint(
        val viewerLocation: Coordinate,
        val viewerAltitude: Float,
        val coordinate: AugmentedRealityCoordinate,
        val angularDiameter: Float
    )

    @Volatile
    private var cachedPoint: CalculatedPoint? = null

    override fun getAngularDiameter(view: AugmentedRealityView): Float {
        return getCalculatedPoint(view).angularDiameter
    }

    override fun getAugmentedRealityCoordinate(view: AugmentedRealityView): AugmentedRealityCoordinate {
        return getCalculatedPoint(view).coordinate
    }

    private fun getCalculatedPoint(view: AugmentedRealityView): CalculatedPoint {
        val viewerLocation = view.location
        val viewerAltitude = view.altitude
        val cached = cachedPoint
        if (cached != null && cached.viewerLocation == viewerLocation &&
            cached.viewerAltitude == viewerAltitude
        ) {
            return cached
        }

        val actualElevation = getActualElevation(viewerAltitude)
        val elevationDifference = actualElevation - viewerAltitude
        val horizontalDistance = viewerLocation.distanceTo(location)
        val elevationAngle = if (abs(elevationDifference) < 0.0001f) {
            0f
        } else {
            atan2(elevationDifference, horizontalDistance).toDegrees()
        }
        val point = CalculatedPoint(
            viewerLocation,
            viewerAltitude,
            AugmentedRealityCoordinate(
                AugmentedRealityUtils.toEastNorthUp(
                    viewerLocation.bearingTo(location).value,
                    elevationAngle,
                    hypot(horizontalDistance, elevationDifference)
                ),
                true
            ),
            AugmentedRealityUtils.getAngularSize(
                actualDiameter,
                hypot(horizontalDistance, elevationDifference)
            )
        )
        cachedPoint = point
        return point
    }

    private fun getActualElevation(viewerAltitude: Float): Float {
        return if (isElevationRelative) {
            viewerAltitude + (elevation ?: 0f)
        } else {
            elevation ?: viewerAltitude
        }
    }
}
