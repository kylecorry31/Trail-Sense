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
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.SizeUnit
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

fun GeoJsonFeature.getNumberProperty(property: String): Number? {
    return properties?.get(property) as? Number
}

fun GeoJsonFeature.getIntProperty(property: String): Int? {
    return getNumberProperty(property)?.toInt()
}

fun GeoJsonFeature.getStringProperty(property: String): String? {
    return properties?.get(property) as? String
}

fun GeoJsonFeature.getBooleanProperty(property: String): Boolean? {
    return properties?.get(property) as? Boolean
}

fun GeoJsonFeature.getFloatProperty(property: String): Float? {
    return getNumberProperty(property)?.toFloat()
}

fun GeoJsonFeature.getColor(): Int? {
    return getIntProperty(GEO_JSON_PROPERTY_COLOR)
}

fun GeoJsonFeature.getName(): String? {
    return getStringProperty(GEO_JSON_PROPERTY_NAME)
}

// POINT ONLY
fun GeoJsonFeature.getStrokeColor(): Int? {
    return getIntProperty(GEO_JSON_PROPERTY_STROKE_COLOR)
}

// TODO: Use a preset list of icons
fun GeoJsonFeature.getIcon(): Int? {
    return getIntProperty(GEO_JSON_PROPERTY_ICON)
}

fun GeoJsonFeature.getIconColor(): Int? {
    return getIntProperty(GEO_JSON_PROPERTY_ICON_COLOR)
}

fun GeoJsonFeature.getIconSize(): Float? {
    return getFloatProperty(GEO_JSON_PROPERTY_ICON_SIZE)
}

fun GeoJsonFeature.getOpacity(): Int {
    return getIntProperty(GEO_JSON_PROPERTY_OPACITY) ?: 255
}

fun GeoJsonFeature.getSize(): Float? {
    return getFloatProperty(GEO_JSON_PROPERTY_SIZE)
}

fun GeoJsonFeature.getStrokeWeight(): Float? {
    return getFloatProperty(GEO_JSON_PROPERTY_STROKE_WEIGHT)
}


fun GeoJsonFeature.getSizeUnit(): SizeUnit {
    return when (getStringProperty(GEO_JSON_PROPERTY_SIZE_UNIT)) {
        GEO_JSON_PROPERTY_SIZE_UNIT_METERS -> SizeUnit.Meters
        GEO_JSON_PROPERTY_SIZE_UNIT_PIXELS -> SizeUnit.Pixels
        else -> SizeUnit.DensityPixels
    }
}

fun GeoJsonFeature.useScale(): Boolean {
    return getBooleanProperty(GEO_JSON_PROPERTY_USE_SCALE) ?: true
}

fun GeoJsonFeature.isClickable(): Boolean {
    return getBooleanProperty(GEO_JSON_PROPERTY_IS_CLICKABLE) ?: false
}

fun GeoJsonFeature.getMarkerShape(): String? {
    return getStringProperty(GEO_JSON_PROPERTY_MARKER_SHAPE)
}


// LINE STRING ONLY
fun GeoJsonFeature.getLineStyle(): LineStyle? {
    val lineStyleId = getIntProperty(GEO_JSON_PROPERTY_LINE_STYLE)
    return LineStyle.entries.firstOrNull { it.id == lineStyleId }
}

fun GeoJsonFeature.getThicknessScale(): Float? {
    return getFloatProperty(GEO_JSON_PROPERTY_LINE_THICKNESS_SCALE)
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

fun GeoJsonFeature.Companion.point(
    point: Coordinate,
    id: Long? = null,
    name: String? = null,
    color: Int? = null,
    icon: Int? = null,
    iconColor: Int? = null,
    markerShape: String? = GEO_JSON_PROPERTY_MARKER_SHAPE_CIRCLE,
    isClickable: Boolean = false,
    strokeColor: Int? = null,
    strokeWeight: Float? = null,
    opacity: Int? = null,
    useScale: Boolean? = null,
    size: Float? = null,
    iconSize: Float? = size,
    sizeUnit: String? = null,
    bounds: CoordinateBounds? = CoordinateBounds(
        point.latitude,
        point.longitude,
        point.latitude,
        point.longitude
    ),
    additionalProperties: Map<String, Any?> = emptyMap()
): GeoJsonFeature {
    val boundingBox = bounds?.let { createBoundingBox(it) }
    return GeoJsonFeature(
        id,
        GeoJsonPoint(GeoJsonPosition(point.longitude, point.latitude), boundingBox),
        mapOf(
            GEO_JSON_PROPERTY_NAME to name,
            GEO_JSON_PROPERTY_SIZE to size,
            GEO_JSON_PROPERTY_SIZE_UNIT to sizeUnit,
            GEO_JSON_PROPERTY_ICON to icon,
            GEO_JSON_PROPERTY_ICON_SIZE to iconSize,
            GEO_JSON_PROPERTY_ICON_COLOR to iconColor,
            GEO_JSON_PROPERTY_COLOR to color,
            GEO_JSON_PROPERTY_MARKER_SHAPE to markerShape,
            GEO_JSON_PROPERTY_IS_CLICKABLE to isClickable,
            GEO_JSON_PROPERTY_STROKE_COLOR to strokeColor,
            GEO_JSON_PROPERTY_STROKE_WEIGHT to strokeWeight,
            GEO_JSON_PROPERTY_OPACITY to opacity,
            GEO_JSON_PROPERTY_USE_SCALE to useScale
        ) + additionalProperties,
        boundingBox = boundingBox
    )
}

private fun createBoundingBox(bounds: CoordinateBounds): GeoJsonBoundingBox {
    return GeoJsonBoundingBox(bounds.west, bounds.south, bounds.east, bounds.north)
}

const val GEO_JSON_PROPERTY_COLOR = "color"
const val GEO_JSON_PROPERTY_STROKE_COLOR = "strokeColor"
const val GEO_JSON_PROPERTY_STROKE_WEIGHT = "strokeWeight"
const val GEO_JSON_PROPERTY_NAME = "name"
const val GEO_JSON_PROPERTY_MARKER_SHAPE = "markerShape"
const val GEO_JSON_PROPERTY_MARKER_SHAPE_NONE = "none"
const val GEO_JSON_PROPERTY_MARKER_SHAPE_CIRCLE = "circle"
const val GEO_JSON_PROPERTY_ICON = "icon"
const val GEO_JSON_PROPERTY_ICON_COLOR = "iconColor"
const val GEO_JSON_PROPERTY_ICON_SIZE = "iconSize"
const val GEO_JSON_PROPERTY_SIZE = "size"
const val GEO_JSON_PROPERTY_OPACITY = "opacity"
const val GEO_JSON_PROPERTY_LINE_STYLE = "lineStyle"
const val GEO_JSON_PROPERTY_LINE_THICKNESS_SCALE = "thicknessScale"
const val GEO_JSON_PROPERTY_USE_SCALE = "useScale"
const val GEO_JSON_PROPERTY_IS_CLICKABLE = "isClickable"
const val GEO_JSON_PROPERTY_SIZE_UNIT = "sizeUnit"
const val GEO_JSON_PROPERTY_SIZE_UNIT_PIXELS = "px"
const val GEO_JSON_PROPERTY_SIZE_UNIT_DENSITY_PIXELS = "dp"
const val GEO_JSON_PROPERTY_SIZE_UNIT_METERS = "m"
