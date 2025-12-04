package com.kylecorry.trail_sense.shared.extensions

import android.graphics.Color
import com.kylecorry.andromeda.geojson.GeoJsonBoundingBox
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
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

fun GeoJsonObject.normalize(): List<GeoJsonFeature> {
    return when (this) {
        is GeoJsonFeatureCollection -> normalize()
        is GeoJsonFeature -> normalize()
        is GeoJsonGeometry -> normalize().map { GeoJsonFeature(null, it, null, it.boundingBox) }
        else -> emptyList()
    }
}

fun GeoJsonFeatureCollection.normalize(): List<GeoJsonFeature> {
    return features.flatMap { it.normalize() }
}

fun GeoJsonFeature.normalize(): List<GeoJsonFeature> {
    if (geometry == null) {
        return emptyList()
    }
    return geometry!!.normalize().map {
        GeoJsonFeature(id, it, properties, it.boundingBox ?: boundingBox)
    }
}

fun GeoJsonGeometry.normalize(): List<GeoJsonGeometry> {
    val geometry = mutableListOf<GeoJsonGeometry>()
    when (this) {
        is GeoJsonGeometryCollection -> {
            geometry.addAll(this.geometries)
        }

        is GeoJsonMultiLineString -> {
            lines?.forEach {
                geometry.add(GeoJsonLineString(it, boundingBox))
            }
        }

        is GeoJsonMultiPoint -> {
            points?.forEach {
                geometry.add(GeoJsonPoint(it, boundingBox))
            }
        }

        is GeoJsonMultiPolygon -> {
            polygons?.forEach {
                geometry.add(GeoJsonPolygon(it, boundingBox))
            }
        }

        else -> {
            geometry.add(this)
        }
    }
    return geometry
}

fun GeoJsonFeature.getColor(): Int? {
    return properties?.get(GEO_JSON_PROPERTY_COLOR) as? Int
}

fun GeoJsonFeature.getLineStyle(): LineStyle? {
    val lineStyleId = properties?.get(GEO_JSON_PROPERTY_LINE_STYLE) as? Int
    return LineStyle.entries.firstOrNull { it.id == lineStyleId }
}

fun GeoJsonFeature.getName(): String? {
    return properties?.get(GEO_JSON_PROPERTY_NAME) as? String
}

fun GeoJsonFeature.getThicknessScale(): Float? {
    return properties?.get(GEO_JSON_PROPERTY_LINE_THICKNESS_SCALE) as? Float
}

fun GeoJsonFeature.Companion.lineString(
    points: List<Coordinate>,
    id: Long? = null,
    name: String? = null,
    lineStyle: LineStyle = LineStyle.Solid,
    color: Int = Color.WHITE,
    thicknessScale: Float = 1f,
    bounds: CoordinateBounds? = CoordinateBounds.from(points)
): GeoJsonFeature {
    return GeoJsonFeature(
        id,
        GeoJsonLineString(points.map { point ->
            GeoJsonPosition(point.longitude, point.latitude)
        }),
        mapOf(
            GEO_JSON_PROPERTY_NAME to name,
            GEO_JSON_PROPERTY_LINE_STYLE to lineStyle.id,
            GEO_JSON_PROPERTY_COLOR to color,
            GEO_JSON_PROPERTY_LINE_THICKNESS_SCALE to thicknessScale
        ),
        boundingBox = bounds?.let { createBoundingBox(it) }
    )
}

private fun createBoundingBox(bounds: CoordinateBounds): GeoJsonBoundingBox {
    return GeoJsonBoundingBox(bounds.west, bounds.south, bounds.east, bounds.north)
}

const val GEO_JSON_PROPERTY_COLOR = "color"
const val GEO_JSON_PROPERTY_NAME = "name"
const val GEO_JSON_PROPERTY_ICON = "icon"
const val GEO_JSON_PROPERTY_LINE_STYLE = "lineStyle"
const val GEO_JSON_PROPERTY_LINE_THICKNESS_SCALE = "thicknessScale"
