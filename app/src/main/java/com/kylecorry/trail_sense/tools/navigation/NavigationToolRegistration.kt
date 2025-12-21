package com.kylecorry.trail_sense.tools.navigation

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourMapLayerPreferences
import com.kylecorry.trail_sense.shared.dem.map_layers.ElevationMapLayerPreferences
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeMapLayerPreferences
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconMapLayerPreferences
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationMapLayerPreferences
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.paths.map_layers.PathMapLayerPreferences
import com.kylecorry.trail_sense.tools.photo_maps.map_layers.PhotoMapMapLayerPreferences
import com.kylecorry.trail_sense.tools.signal_finder.map_layers.CellTowerMapLayerPreferences
import com.kylecorry.trail_sense.tools.tides.map_layers.TideMapLayerPreferences
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolMap
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object NavigationToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.NAVIGATION,
            context.getString(R.string.navigation),
            R.drawable.ic_compass_icon,
            R.id.action_navigation,
            ToolCategory.Location,
            guideId = R.raw.guide_tool_navigation,
            settingsNavAction = R.id.navigationSettingsFragment,
            diagnostics = listOf(
                *ToolDiagnosticFactory.sightingCompass(context),
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.altimeter(context),
                ToolDiagnosticFactory.pedometer(context),
            ).distinctBy { it.id },
            singletons = listOf(Navigator::getInstance),
            maps = listOf(
                ToolMap(
                    MAP_ID, listOf(
                        ElevationMapLayerPreferences(context, MAP_ID),
                        HillshadeMapLayerPreferences(context, MAP_ID),
                        PhotoMapMapLayerPreferences(context, MAP_ID),
                        ContourMapLayerPreferences(context, MAP_ID),
                        CellTowerMapLayerPreferences(context, MAP_ID),
                        PathMapLayerPreferences(context, MAP_ID),
                        BeaconMapLayerPreferences(context, MAP_ID),
                        TideMapLayerPreferences(context, MAP_ID),
                        MyLocationMapLayerPreferences(context, MAP_ID)
                    )
                )
            )
        )
    }

    const val MAP_ID = "navigation"
}