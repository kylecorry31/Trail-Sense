package com.kylecorry.trail_sense.shared.sharing

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonPoint
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.getLayerId
import com.kylecorry.trail_sense.shared.extensions.getLongProperty
import com.kylecorry.trail_sense.shared.extensions.getName
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconGeoJsonSource
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object GeoJsonFeatureClickHandler {

    fun handleFeatureClick(
        fragment: Fragment,
        feature: GeoJsonFeature
    ) {
        val layerId = feature.getLayerId()

        val bundle = Bundle().apply {
            putString(MapToolRegistration.BROADCAST_PARAM_GEOJSON_FEATURE_ID, feature.id?.toString())
            putString(MapToolRegistration.BROADCAST_PARAM_GEOJSON_LAYER_ID, layerId)
        }
        Tools.broadcast(MapToolRegistration.BROADCAST_GEOJSON_FEATURE_SELECTION_CHANGED, bundle)

        val formatter = AppServiceRegistry.get<FormatService>()
        val location = getFeatureLocation(feature) ?: return
        val name = feature.getName()
        val title = name ?: fragment.getString(R.string.location)
        val layer = if (layerId != null) {
            Tools.getTools(fragment.requireContext())
                .flatMap { it.mapLayers }
                .firstOrNull { it.id == layerId }
        } else {
            null
        }

        Share.share(
            fragment,
            title,
            listOfNotNull(
                ShareAction.Navigate,
                if (layer?.openFeature != null) {
                    ShareAction.Open
                } else {
                    null
                }
            ),
            subtitle = formatter.formatLocation(location)
        ) { action ->
            when (action) {
                ShareAction.Navigate -> {
                    val navigator = AppServiceRegistry.get<Navigator>()
                    val beaconId = feature.getLongProperty(BeaconGeoJsonSource.PROPERTY_BEACON_ID)
                    if (beaconId != null) {
                        navigator.navigateTo(beaconId)
                    } else {
                        navigator.navigateTo(location, name ?: "", BeaconOwner.Maps)
                    }
                }

                ShareAction.Open -> {
                    layer?.openFeature?.invoke(feature, fragment)
                }

                else -> {
                    // Do nothing
                }
            }

            // Broadcast deselection after the action
            val deselectBundle = Bundle().apply {
                putString(MapToolRegistration.BROADCAST_PARAM_GEOJSON_FEATURE_ID, null)
                putString(MapToolRegistration.BROADCAST_PARAM_GEOJSON_LAYER_ID, layerId)
            }
            Tools.broadcast(
                MapToolRegistration.BROADCAST_GEOJSON_FEATURE_SELECTION_CHANGED,
                deselectBundle
            )
        }
    }

    private fun getFeatureLocation(feature: GeoJsonFeature): Coordinate? {
        val point = feature.geometry as? GeoJsonPoint ?: return null
        return point.point?.coordinate
    }
}
