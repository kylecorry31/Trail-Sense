package com.kylecorry.trail_sense.tools.beacons

import android.content.Context
import android.os.Build
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.shared.extensions.getLongProperty
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconGeoJsonSource
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconLayer
import com.kylecorry.trail_sense.tools.beacons.quickactions.QuickActionPlaceBeacon
import com.kylecorry.trail_sense.tools.beacons.widgets.AppWidgetNearbyBeacons
import com.kylecorry.trail_sense.tools.beacons.widgets.NearbyBeaconsToolWidgetView
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIntentHandler
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object BeaconsToolRegistration : ToolRegistration {

    private val geoIntentHandler =
        ToolIntentHandler { activity, intent ->
            val intentData = intent.data ?: return@ToolIntentHandler false
            if (intent.scheme != "geo") return@ToolIntentHandler false
            val geo = GeoUri.from(intentData) ?: return@ToolIntentHandler false

            val bundle = bundleOf("initial_location" to geo)
            activity.findNavController().navigate(
                R.id.beacon_list,
                bundle
            )
            true
        }

    private val openBeaconIntentHandler =
        ToolIntentHandler { activity, intent ->
            val beaconId = intent.getLongExtra("beacon_id", -1L)
            if (beaconId == -1L) return@ToolIntentHandler false
            val bundle = bundleOf("beacon_id" to beaconId)
            activity.findNavController().navigate(
                R.id.beaconDetailsFragment,
                bundle
            )
            true
        }

    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.BEACONS,
            context.getString(R.string.beacons),
            R.drawable.ic_location,
            R.id.beacon_list,
            ToolCategory.Location,
            guideId = R.raw.guide_tool_beacons,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_CREATE_BEACON,
                    context.getString(R.string.create_beacon),
                    ::QuickActionPlaceBeacon
                )
            ),
            additionalNavigationIds = listOf(
                R.id.beaconDetailsFragment,
                R.id.placeBeaconFragment
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.altimeter(context),
                ToolDiagnosticFactory.camera(context),
                *ToolDiagnosticFactory.sightingCompass(context)
            ).distinctBy { it.id },
            intentHandlers = listOf(geoIntentHandler, openBeaconIntentHandler),
            broadcasts = listOf(
                ToolBroadcast(BROADCAST_BEACONS_CHANGED, "Beacons changed")
            ),
            singletons = listOf(
                { BeaconService(it) }
            ),
            widgets = listOf(
                ToolWidget(
                    WIDGET_NEARBY_BEACONS,
                    context.getString(R.string.nearby_beacons),
                    ToolSummarySize.Full,
                    NearbyBeaconsToolWidgetView(),
                    AppWidgetNearbyBeacons::class.java,
                    updateBroadcasts = listOf(
                        BROADCAST_BEACONS_CHANGED,
                        SensorsToolRegistration.BROADCAST_LOCATION_CHANGED
                    ),
                    usesLocation = true,
                    canPlaceInApp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                )
            ),
            mapLayers = listOf(
                MapLayerDefinition(
                    MAP_LAYER_BEACONS,
                    context.getString(R.string.beacons),
                    description = context.getString(R.string.map_layer_beacons_description),
                    openFeature = { feature, fragment ->
                        val beaconId = feature.getLongProperty(BeaconGeoJsonSource.PROPERTY_BEACON_ID)
                        val navController = fragment.findNavController()
                        navController.navigateWithAnimation(
                            R.id.beaconDetailsFragment,
                            bundleOf(
                                "beacon_id" to beaconId
                            )
                        )
                    }
                ) { BeaconLayer() }
            )
        )
    }

    const val BROADCAST_BEACONS_CHANGED = "beacons-broadcast-beacons-changed"
    const val WIDGET_NEARBY_BEACONS = "beacons-widget-nearby-beacons"
    const val MAP_LAYER_BEACONS = "beacon"
}
