package com.kylecorry.trail_sense.tools.photo_maps

import android.content.Context
import androidx.core.os.bundleOf
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourMapLayerPreferences
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconMapLayerPreferences
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationMapLayerPreferences
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationMapLayerPreferences
import com.kylecorry.trail_sense.tools.paths.map_layers.PathMapLayerPreferences
import com.kylecorry.trail_sense.tools.photo_maps.infrastructure.MapRepo
import com.kylecorry.trail_sense.tools.photo_maps.quickactions.QuickActionOpenPhotoMap
import com.kylecorry.trail_sense.tools.signal_finder.map_layers.CellTowerMapLayerPreferences
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayerPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIntentHandler
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolMap
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object PhotoMapsToolRegistration : ToolRegistration {

    private val importMapIntentHandler = ToolIntentHandler { activity, intent ->
        val validTypes = listOf("image/", "application/pdf")
        if (!validTypes.any { intent.type?.startsWith(it) == true }) {
            return@ToolIntentHandler false
        }

        val intentUri = intent.clipData?.getItemAt(0)?.uri ?: return@ToolIntentHandler false
        val bundle = bundleOf("map_intent_uri" to intentUri)
        activity.findNavController().navigate(R.id.mapListFragment, bundle)
        true
    }

    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.PHOTO_MAPS,
            context.getString(R.string.photo_maps),
            R.drawable.photo_maps,
            R.id.mapListFragment,
            ToolCategory.Location,
            context.getString(R.string.photo_map_summary),
            guideId = R.raw.guide_tool_photo_maps,
            settingsNavAction = R.id.photoMapSettingsFragment,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_OPEN_PHOTO_MAP,
                    context.getString(R.string.open_photo_map),
                    ::QuickActionOpenPhotoMap
                )

            ),
            additionalNavigationIds = listOf(
                R.id.photoMapsFragment
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                ToolDiagnosticFactory.camera(context),
                *ToolDiagnosticFactory.compass(context)
            ).distinctBy { it.id },
            intentHandlers = listOf(importMapIntentHandler),
            singletons = listOf(
                MapRepo::getInstance
            ),
            maps = listOf(
                ToolMap(
                    MAP_ID, listOf(
                        ContourMapLayerPreferences(context, MAP_ID),
                        CellTowerMapLayerPreferences(context, MAP_ID),
                        PathMapLayerPreferences(context, MAP_ID),
                        BeaconMapLayerPreferences(context, MAP_ID),
                        NavigationMapLayerPreferences(context, MAP_ID),
                        TideMapLayerPreferences(context, MAP_ID),
                        MyLocationMapLayerPreferences(context, MAP_ID)
                    )
                )
            )
        )
    }

    const val MAP_ID = "photo_maps"
}