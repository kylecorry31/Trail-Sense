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

object PluginGeoJsonValidator {

    fun isValid(obj: GeoJsonObject): Boolean {
        val counts = Counts()
        return isValid(obj, counts) &&
                counts.features <= PluginGuard.MAX_GEOJSON_FEATURES &&
                counts.coordinates <= PluginGuard.MAX_GEOJSON_COORDINATES
    }

    private fun isValid(obj: GeoJsonObject, counts: Counts): Boolean {
        return when (obj) {
            is GeoJsonFeatureCollection -> {
                if (obj.features.size > PluginGuard.MAX_GEOJSON_FEATURES) {
                    return false
                }
                obj.features.all { isValid(it, counts) }
            }

            is GeoJsonFeature -> {
                counts.features++
                counts.features <= PluginGuard.MAX_GEOJSON_FEATURES &&
                        obj.geometry?.let { isValid(it, counts, 0) } != false
            }

            is GeoJsonGeometry -> isValid(obj, counts, 0)
            else -> false
        }
    }

    private fun isValid(geometry: GeoJsonGeometry, counts: Counts, depth: Int): Boolean {
        val hasValidDepth = depth <= PluginGuard.MAX_GEOJSON_GEOMETRY_DEPTH
        if (hasValidDepth && geometry is GeoJsonGeometryCollection) {
            return isValidGeometryCollection(geometry, counts, depth)
        }

        if (hasValidDepth) {
            counts.coordinates += getCoordinateCount(geometry)
        }
        return hasValidDepth && counts.coordinates <= PluginGuard.MAX_GEOJSON_COORDINATES
    }

    private fun isValidGeometryCollection(
        geometry: GeoJsonGeometryCollection,
        counts: Counts,
        depth: Int
    ): Boolean {
        return geometry.geometries.size <= PluginGuard.MAX_GEOJSON_FEATURES &&
                geometry.geometries.all { isValid(it, counts, depth + 1) }
    }

    private fun getCoordinateCount(geometry: GeoJsonGeometry): Int {
        return getSimpleCoordinateCount(geometry)
            ?: getMultiCoordinateCount(geometry)
            ?: 0
    }

    private fun getSimpleCoordinateCount(geometry: GeoJsonGeometry): Int? {
        return when (geometry) {
            is GeoJsonPoint -> if (geometry.point == null) 0 else 1
            is GeoJsonLineString -> geometry.line?.size ?: 0
            is GeoJsonPolygon -> geometry.polygon?.sumOf { it.size } ?: 0
            else -> null
        }
    }

    private fun getMultiCoordinateCount(geometry: GeoJsonGeometry): Int? {
        return when (geometry) {
            is GeoJsonMultiPoint -> geometry.points?.size ?: 0
            is GeoJsonMultiLineString -> geometry.lines?.sumOf { it.size } ?: 0
            is GeoJsonMultiPolygon -> geometry.polygons?.sumOf { polygon ->
                polygon.sumOf { it.size }
            } ?: 0
            is GeoJsonGeometryCollection -> 0
            else -> null
        }
    }

    private data class Counts(
        var features: Int = 0,
        var coordinates: Int = 0
    )
}
