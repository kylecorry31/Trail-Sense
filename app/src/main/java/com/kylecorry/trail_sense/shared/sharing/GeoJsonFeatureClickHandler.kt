package com.kylecorry.trail_sense.shared.sharing

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonPoint
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.getLongProperty
import com.kylecorry.trail_sense.shared.extensions.getName
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconLayer
import com.kylecorry.trail_sense.tools.field_guide.map_layers.FieldGuideSightingLayer
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator

object GeoJsonFeatureClickHandler {

    fun handleFeatureClick(
        fragment: Fragment,
        feature: GeoJsonFeature
    ) {
        val formatter = AppServiceRegistry.get<FormatService>()
        val location = getFeatureLocation(feature) ?: return
        val name = feature.getName()
        val title = name ?: fragment.getString(R.string.location)

        // TODO: Handlers registered in the tool registration
        val beaconId = feature.getLongProperty(BeaconLayer.PROPERTY_BEACON_ID)
        val fieldGuidePageId = feature.getLongProperty(FieldGuideSightingLayer.PROPERTY_PAGE_ID)

        Share.share(
            fragment,
            title,
            listOfNotNull(
                ShareAction.Navigate,
                if (beaconId != null || fieldGuidePageId != null) {
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
                    if (beaconId != null) {
                        navigator.navigateTo(beaconId)
                    } else {
                        navigator.navigateTo(location, name ?: "", BeaconOwner.Maps)
                    }
                }

                ShareAction.Open -> {
                    val navController = fragment.findNavController()
                    when {
                        beaconId != null -> {
                            navController.navigateWithAnimation(
                                R.id.beaconDetailsFragment,
                                bundleOf(
                                    "beacon_id" to beaconId
                                )
                            )
                        }

                        fieldGuidePageId != null -> {
                            navController.navigateWithAnimation(
                                R.id.fieldGuidePageFragment,
                                bundleOf(
                                    "page_id" to fieldGuidePageId
                                )
                            )
                        }
                    }
                }

                else -> {
                    // Do nothing
                }
            }
        }
    }

    private fun getFeatureLocation(feature: GeoJsonFeature): Coordinate? {
        val point = feature.geometry as? GeoJsonPoint ?: return null
        return point.point?.coordinate
    }
}
