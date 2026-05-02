package com.kylecorry.trail_sense.plugins.infrastructure

import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonGeometry
import com.kylecorry.andromeda.geojson.GeoJsonGeometryCollection
import com.kylecorry.andromeda.geojson.GeoJsonLineString
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.andromeda.geojson.GeoJsonPoint
import com.kylecorry.andromeda.geojson.GeoJsonPosition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class PluginGuardTest {

    @ParameterizedTest
    @CsvSource(
        "/map_layers/weather-1, true",
        "map_layers/weather, false",
        "/map layers/weather, false",
        "/123456790123456790123456790123456790123456790123456790123456790123456790123456790123456790123456790, true",
        "/1234567901234567901234567901234567901234567901234567901234567901234567901234567901234567901234567901, false",
        "/, false"
    )
    fun isValidEndpoint(endpoint: String, isValid: Boolean) {
        assertEquals(isValid, PluginGuard.isValidEndpoint(endpoint))
    }

    @ParameterizedTest
    @CsvSource(
        "0, true",
        "1, false",
        ", false"
    )
    fun isValidRegistrationPayload(sizeOffset: Int?, isValid: Boolean) {
        val payload = sizeOffset?.let { ByteArray(PluginGuard.MAX_REGISTRATION_BYTES + it) }

        assertEquals(isValid, PluginGuard.isValidRegistrationPayload(payload))
    }

    @ParameterizedTest
    @CsvSource(
        "0, true",
        "1, false"
    )
    fun isValidGeoJsonPayload(sizeOffset: Int, isValid: Boolean) {
        val payload = ByteArray(PluginGuard.MAX_GEOJSON_BYTES + sizeOffset)

        assertEquals(isValid, PluginGuard.isValidGeoJsonPayload(payload))
    }

    @ParameterizedTest
    @CsvSource(
        "0, true",
        "1, false"
    )
    fun isValidTilePayload(sizeOffset: Int, isValid: Boolean) {
        val payload = ByteArray(PluginGuard.MAX_TILE_BYTES + sizeOffset)

        assertEquals(isValid, PluginGuard.isValidTilePayload(payload))
    }

    @ParameterizedTest
    @CsvSource(
        "max, max, true",
        "0, max, false",
        "max, 0, false",
        "over, max, false",
        "max, over, false",
        "0, 0, false",
        "1, 1, true",
        "over, over, false"
    )
    fun isValidTileSize(widthValue: String, heightValue: String, isValid: Boolean) {
        val width = tileSize(widthValue)
        val height = tileSize(heightValue)

        assertEquals(isValid, PluginGuard.isValidTileSize(width, height))
    }

    @ParameterizedTest
    @MethodSource("provideGeoJson")
    fun isValidGeoJson(geojson: GeoJsonObject, isValid: Boolean) {
        assertEquals(isValid, PluginGuard.isValidGeoJson(geojson))
    }

    companion object {
        @JvmStatic
        fun provideGeoJson(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    GeoJsonFeatureCollection(
                        listOf(
                            GeoJsonFeature(null, point(), emptyMap())
                        )
                    ),
                    true
                ),
                Arguments.of(
                    GeoJsonFeatureCollection(
                        List(PluginGuard.MAX_GEOJSON_FEATURES) {
                            GeoJsonFeature(it, point(), emptyMap())
                        }
                    ),
                    true
                ),
                Arguments.of(
                    GeoJsonFeatureCollection(
                        List(PluginGuard.MAX_GEOJSON_FEATURES + 1) {
                            GeoJsonFeature(it, point(), emptyMap())
                        }
                    ),
                    false
                ),
                Arguments.of(
                    GeoJsonLineString(positions(PluginGuard.MAX_GEOJSON_COORDINATES)),
                    true
                ),
                Arguments.of(
                    GeoJsonLineString(positions(PluginGuard.MAX_GEOJSON_COORDINATES + 1)),
                    false
                ),
                Arguments.of(
                    nestedCollection(PluginGuard.MAX_GEOJSON_GEOMETRY_DEPTH),
                    true
                ),
                Arguments.of(
                    nestedCollection(PluginGuard.MAX_GEOJSON_GEOMETRY_DEPTH + 1),
                    false
                ),
                Arguments.of(
                    GeoJsonGeometryCollection(
                        List(PluginGuard.MAX_GEOJSON_FEATURES + 1) {
                            point()
                        }
                    ),
                    false
                ),
                Arguments.of(GeoJsonFeature(null, null, emptyMap()), true),
                Arguments.of(UnknownGeoJsonObject(), false)
            )
        }

        private fun nestedCollection(depth: Int): GeoJsonGeometry {
            return if (depth == 0) {
                point()
            } else {
                GeoJsonGeometryCollection(listOf(nestedCollection(depth - 1)))
            }
        }

        private fun point(): GeoJsonPoint {
            return GeoJsonPoint(GeoJsonPosition(0.0, 0.0))
        }

        private fun positions(count: Int): List<GeoJsonPosition> {
            return List(count) {
                GeoJsonPosition(it.toDouble(), it.toDouble())
            }
        }

        private fun tileSize(value: String): Int {
            return when (value) {
                "max" -> PluginGuard.MAX_TILE_SIZE
                "over" -> PluginGuard.MAX_TILE_SIZE + 1
                else -> value.toInt()
            }
        }

        private class UnknownGeoJsonObject : GeoJsonObject {
            override val type: String = "Unknown"
            override val boundingBox = null
        }
    }
}
