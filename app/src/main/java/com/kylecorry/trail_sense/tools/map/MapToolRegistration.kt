package com.kylecorry.trail_sense.tools.map

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourMapLayerPreferences
import com.kylecorry.trail_sense.shared.dem.map_layers.ElevationMapLayerPreferences
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeMapLayerPreferences
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconMapLayerPreferences
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapMapLayerPreferences
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationMapLayerPreferences
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationMapLayerPreferences
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

object MapToolRegistration : ToolRegistration {

    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.MAP,
            context.getString(R.string.map),
            R.drawable.maps,
            R.id.mapFragment,
            ToolCategory.Location,
            settingsNavAction = R.id.mapSettingsFragment,
            guideId = R.raw.guide_tool_map,
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.compass(context)
            ).distinctBy { it.id },
            maps = listOf(
                ToolMap(
                    MAP_ID, listOf(
                        BaseMapMapLayerPreferences(context, MAP_ID),
                        ElevationMapLayerPreferences(context, MAP_ID, isEnabledByDefault = true),
                        HillshadeMapLayerPreferences(context, MAP_ID, isEnabledByDefault = true),
                        PhotoMapMapLayerPreferences(context, MAP_ID, defaultOpacity = 100),
                        ContourMapLayerPreferences(context, MAP_ID, isEnabledByDefault = true),
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

    val MAP_ID = "map"
}