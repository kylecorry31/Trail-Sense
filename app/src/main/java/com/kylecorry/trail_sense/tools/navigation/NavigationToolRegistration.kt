package com.kylecorry.trail_sense.tools.navigation

import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceType
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationGeoJsonSource
import com.kylecorry.trail_sense.tools.paths.ui.PathBackgroundColor
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
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
            initialize = {
                DependencyRegistry.addSingleton(Navigator.getInstance(it))
            },
            mapLayers = listOf(
                MapLayerDefinition(
                    NavigationGeoJsonSource.SOURCE_ID,
                    context.getString(R.string.navigation),
                    description = context.getString(R.string.map_layer_navigation_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = DefaultMapLayerDefinitions.BACKGROUND_COLOR,
                            title = context.getString(R.string.background_color),
                            type = MapLayerPreferenceType.Enum,
                            values = listOf(
                                context.getString(R.string.none) to PathBackgroundColor.None.id.toString(),
                                context.getString(R.string.color_black) to PathBackgroundColor.Black.id.toString(),
                                context.getString(R.string.color_white) to PathBackgroundColor.White.id.toString(),
                            ),
                            defaultValue = PathBackgroundColor.None.id.toString(),
                        ),
                        MapLayerPreference(
                            id = NavigationGeoJsonSource.SHOW_ENDPOINTS,
                            title = context.getString(R.string.map_layer_navigation_show_endpoints),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = false
                        )
                    ),
                    geoJsonSource = ::NavigationGeoJsonSource,
                    refreshBroadcasts = listOf(
                        SensorsToolRegistration.BROADCAST_LOCATION_CHANGED,
                        BROADCAST_DESTINATION_CHANGED
                    )
                )
            ),
            broadcasts = listOf(
                ToolBroadcast(
                    BROADCAST_DESTINATION_CHANGED,
                    "Destination changed"
                )
            )
        )
    }

    const val MAP_ID = "navigation"
    const val BROADCAST_DESTINATION_CHANGED = "navigation-broadcast-destination-changed"
}
