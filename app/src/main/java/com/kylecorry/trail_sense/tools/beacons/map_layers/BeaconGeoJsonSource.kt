package com.kylecorry.trail_sense.tools.beacons.map_layers

import android.graphics.Color
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService

class BeaconGeoJsonSource : GeoJsonSource {

    private var highlighted: Beacon? = null
    private var featureToBeaconMap = mapOf<GeoJsonFeature, Beacon>()
    private val outlineColor = Color.WHITE

    fun highlight(beacon: Beacon?) {
        highlighted = beacon
    }

    private val beaconService = AppServiceRegistry.get<BeaconService>()

    fun getBeacon(feature: GeoJsonFeature): Beacon? {
        return featureToBeaconMap[feature]
    }

    override suspend fun load(
        bounds: CoordinateBounds,
        metersPerPixel: Float
    ): GeoJsonObject? {
        val beacons =
            (beaconService.getBeaconsInRegion(bounds).filter { it.visible } + listOfNotNull(
                highlighted
            )).distinctBy { it.id }
        val newMap = mutableMapOf<GeoJsonFeature, Beacon>()
        val collection = GeoJsonFeatureCollection(
            beacons.map {
                val point = GeoJsonFeature.point(
                    it.coordinate,
                    it.id,
                    it.name.trim(),
                    color = it.color,
                    strokeColor = outlineColor,
                    opacity = if (highlighted == null || highlighted?.id == it.id) {
                        255
                    } else {
                        127
                    },
                    icon = it.icon?.id,
                    iconSize = 12f * 0.75f,
                    iconColor = Colors.mostContrastingColor(
                        Color.WHITE,
                        Color.BLACK,
                        it.color
                    ),
                    isClickable = true,
                    additionalProperties = mapOf(
                        BeaconLayer.PROPERTY_BEACON_ID to it.id
                    )
                )
                newMap[point] = it
                point
            }
        )
        featureToBeaconMap = newMap
        return collection
    }
}