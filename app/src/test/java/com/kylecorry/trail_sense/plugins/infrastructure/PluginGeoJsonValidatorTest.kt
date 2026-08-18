package com.kylecorry.trail_sense.plugins.infrastructure

import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonGeometry
import com.kylecorry.andromeda.geojson.GeoJsonGeometryCollection
import com.kylecorry.andromeda.geojson.GeoJsonLineString
import com.kylecorry.andromeda.geojson.GeoJsonMultiLineString
import com.kylecorry.andromeda.geojson.GeoJsonMultiPoint
import com.kylecorry.andromeda.geojson.GeoJsonMultiPolygon
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.andromeda.geojson.GeoJsonPoint
import com.kylecorry.andromeda.geojson.GeoJsonPolygon
import com.kylecorry.andromeda.geojson.GeoJsonPosition
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class PluginGeoJsonValidatorTest {

    @ParameterizedTest
    @MethodSource("provideCoordinateCounts")
    fun countsCoordinatesPerGeometryType(atLimit: GeoJsonGeometry, overLimit: GeoJsonGeometry) {
        assertTrue(PluginGeoJsonValidator.isValid(atLimit))
        assertFalse(PluginGeoJsonValidator.isValid(overLimit))
    }

    @ParameterizedTest
    @MethodSource("provideMissingCoordinates")
    fun treatsMissingCoordinatesAsEmpty(geometry: GeoJsonGeometry) {
        assertTrue(PluginGeoJsonValidator.isValid(geometry))
    }

    @Test
    fun countsCoordinatesAcrossFeatures() {
        val perFeature = PluginGuard.MAX_GEOJSON_COORDINATES / 10

        assertTrue(PluginGeoJsonValidator.isValid(featureCollection(10, perFeature)))
        // Each feature is well under the limit, but together they are over it
        assertFalse(PluginGeoJsonValidator.isValid(featureCollection(11, perFeature)))
    }

    @Test
    fun countsCoordinatesAcrossSiblingGeometries() {
        val perGeometry = PluginGuard.MAX_GEOJSON_COORDINATES / 10

        assertTrue(PluginGeoJsonValidator.isValid(geometryCollection(10, perGeometry)))
        assertFalse(PluginGeoJsonValidator.isValid(geometryCollection(11, perGeometry)))
    }

    @Test
    fun countsCoordinatesNestedInGeometryCollections() {
        val nested = GeoJsonGeometryCollection(
            listOf(
                GeoJsonGeometryCollection(
                    listOf(lineString(PluginGuard.MAX_GEOJSON_COORDINATES))
                ),
                lineString(1)
            )
        )

        assertFalse(PluginGeoJsonValidator.isValid(nested))
    }

    @Test
    fun rejectsGeometriesNestedTooDeeplyWithinAFeature() {
        val tooDeep = GeoJsonFeature(
            null,
            nestedCollection(PluginGuard.MAX_GEOJSON_GEOMETRY_DEPTH + 1),
            emptyMap()
        )

        assertFalse(PluginGeoJsonValidator.isValid(tooDeep))
        assertTrue(
            PluginGeoJsonValidator.isValid(
                GeoJsonFeature(
                    null,
                    nestedCollection(PluginGuard.MAX_GEOJSON_GEOMETRY_DEPTH),
                    emptyMap()
                )
            )
        )
    }

    @Test
    fun rejectsUnknownGeoJsonObjects() {
        assertFalse(PluginGeoJsonValidator.isValid(UnknownGeoJsonObject))
    }

    @Test
    fun sizesUnknownNestedGeometriesAsEmpty() {
        assertTrue(
            PluginGeoJsonValidator.isValid(
                GeoJsonGeometryCollection(listOf(UnknownGeoJsonGeometry))
            )
        )
    }

    @Test
    fun allowsAnEmptyFeatureCollection() {
        assertTrue(PluginGeoJsonValidator.isValid(GeoJsonFeatureCollection(emptyList())))
    }

    companion object {

        @JvmStatic
        fun provideCoordinateCounts(): Stream<Arguments> {
            val max = PluginGuard.MAX_GEOJSON_COORDINATES
            return Stream.of(
                Arguments.of(GeoJsonMultiPoint(positions(max)), GeoJsonMultiPoint(positions(max + 1))),
                Arguments.of(polygon(max), polygon(max + 1)),
                Arguments.of(multiLineString(max), multiLineString(max + 1)),
                Arguments.of(multiPolygon(max), multiPolygon(max + 1))
            )
        }

        @JvmStatic
        fun provideMissingCoordinates(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(GeoJsonPoint(null)),
                Arguments.of(GeoJsonLineString(null)),
                Arguments.of(GeoJsonPolygon(null)),
                Arguments.of(GeoJsonMultiPoint(null)),
                Arguments.of(GeoJsonMultiLineString(null)),
                Arguments.of(GeoJsonMultiPolygon(null))
            )
        }

        private fun featureCollection(features: Int, coordinatesEach: Int): GeoJsonFeatureCollection {
            return GeoJsonFeatureCollection(
                List(features) {
                    GeoJsonFeature(it, lineString(coordinatesEach), emptyMap())
                }
            )
        }

        private fun geometryCollection(
            geometries: Int,
            coordinatesEach: Int
        ): GeoJsonGeometryCollection {
            return GeoJsonGeometryCollection(List(geometries) { lineString(coordinatesEach) })
        }

        private fun nestedCollection(depth: Int): GeoJsonGeometry {
            return if (depth == 0) {
                lineString(1)
            } else {
                GeoJsonGeometryCollection(listOf(nestedCollection(depth - 1)))
            }
        }

        private fun lineString(count: Int) = GeoJsonLineString(positions(count))

        // Split across two linear rings to make sure every ring is counted
        private fun polygon(count: Int) =
            GeoJsonPolygon(listOf(positions(count / 2), positions(count - count / 2)))

        private fun multiLineString(count: Int) =
            GeoJsonMultiLineString(listOf(positions(count / 2), positions(count - count / 2)))

        private fun multiPolygon(count: Int) = GeoJsonMultiPolygon(
            listOf(
                listOf(positions(count / 2)),
                listOf(positions(count - count / 2))
            )
        )

        private fun positions(count: Int): List<GeoJsonPosition> {
            return List(count) { GeoJsonPosition(it.toDouble(), it.toDouble()) }
        }

        private object UnknownGeoJsonObject : GeoJsonObject {
            override val type: String = "Unknown"
            override val boundingBox = null
        }

        private object UnknownGeoJsonGeometry : GeoJsonGeometry {
            override val type: String = "Unknown"
            override val boundingBox = null
        }
    }
}
