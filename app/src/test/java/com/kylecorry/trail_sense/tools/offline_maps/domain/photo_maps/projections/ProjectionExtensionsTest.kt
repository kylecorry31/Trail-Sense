package com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.projections

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class ProjectionExtensionsTest {

    @Test
    fun distancePerPixelUsesCoordinateDistanceAndProjectedPixelDistance() {
        val location1 = Coordinate(0.0, 0.0)
        val location2 = Coordinate(1.0, 1.0)
        val projection = ScaledProjection(3f, 4f)

        val distancePerPixel = projection.distancePerPixel(location1, location2)

        assertEquals(
            location1.distanceTo(location2) / 5f,
            distancePerPixel!!.meters().value,
            0.001f
        )
    }

    @Test
    fun distancePerPixelIsNullWhenLocationsAreTheSame() {
        val location = Coordinate(0.0, 0.0)
        val projection = ScaledProjection(3f, 4f)

        assertNull(projection.distancePerPixel(location, location))
    }

    @Test
    fun distancePerPixelIsNullWhenLocationsProjectToTheSamePixel() {
        val location1 = Coordinate(0.0, 0.0)
        val location2 = Coordinate(0.0, 1.0)
        val projection = SamePixelProjection

        assertNull(projection.distancePerPixel(location1, location2))
    }

    private class ScaledProjection(
        private val xScale: Float,
        private val yScale: Float
    ) : IMapProjection {
        override fun toCoordinate(pixel: Vector2): Coordinate {
            return Coordinate(
                latitude = pixel.y / yScale.toDouble(),
                longitude = pixel.x / xScale.toDouble()
            )
        }

        override fun toPixels(location: Coordinate): Vector2 {
            return toPixels(location.latitude, location.longitude)
        }

        override fun toPixels(latitude: Double, longitude: Double): Vector2 {
            return Vector2(
                x = longitude.toFloat() * xScale,
                y = latitude.toFloat() * yScale
            )
        }
    }

    private object SamePixelProjection : IMapProjection {
        override fun toCoordinate(pixel: Vector2): Coordinate {
            return Coordinate.zero
        }

        override fun toPixels(location: Coordinate): Vector2 {
            return Vector2.zero
        }

        override fun toPixels(latitude: Double, longitude: Double): Vector2 {
            return Vector2.zero
        }
    }
}
