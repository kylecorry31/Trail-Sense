package com.kylecorry.trail_sense.shared.data

import android.util.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.stream.Stream

class GeographicImageSourceTest {

    @Test
    fun respectsVerticalResolutionOverride() {
        val size = mock<Size> {
            on { width } doReturn 576
            on { height } doReturn 361
        }
        val reader = mock<DataImageReader> {
            on { getSize() } doReturn size
        }
        val source = GeographicImageSource(
            reader,
            precision = 5,
            verticalResolutionDegrees = 0.5
        )

        assertEquals(0f, source.getPixel(Coordinate(90.0, 0.0)).y)
        assertEquals(180f, source.getPixel(Coordinate(0.0, 0.0)).y)
        assertEquals(359f, source.getPixel(Coordinate(-89.5, 0.0)).y)
        assertEquals(360f, source.getPixel(Coordinate(-90.0, 0.0)).y)
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    fun getLocationIsInverseOfGetPixel(latitude: Double, longitude: Double, offset: Float) {
        val location = Coordinate(latitude, longitude)
        val size = mock<Size> {
            on { width } doReturn 100
            on { height } doReturn 100
        }
        val reader = mock<DataImageReader> {
            on { getSize() } doReturn size
        }
        val bounds = CoordinateBounds(
            north = 10.0,
            south = 0.0,
            east = 10.0,
            west = 0.0
        )
        val source = GeographicImageSource(reader, bounds, precision = 5, valuePixelOffset = offset)

        val pixel = source.getPixel(location)
        val inverseLocation = source.getLocation(pixel)

        assertEquals(location.latitude, inverseLocation.latitude, 0.001, "Latitude failed for $location with offset $offset")
        assertEquals(location.longitude, inverseLocation.longitude, 0.001, "Longitude failed for $location with offset $offset")
    }

    companion object {
        @JvmStatic
        fun provideTestData(): Stream<Arguments> {
            val locations = listOf(
                Coordinate(5.0, 5.0),
                Coordinate(9.9, 0.1),
                Coordinate(9.9, 9.9),
                Coordinate(0.1, 0.1),
                Coordinate(0.1, 9.9)
            )
            val offsets = listOf(0f, 0.5f)

            return locations.flatMap { location ->
                offsets.map { offset ->
                    Arguments.of(location.latitude, location.longitude, offset)
                }
            }.stream()
        }
    }
}
