package com.kylecorry.trail_sense.tools.map

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorStrategy
import com.kylecorry.trail_sense.shared.dem.colors.SlopeColorStrategy
import com.kylecorry.trail_sense.shared.dem.map_layers.AspectLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.ElevationLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.RuggednessLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.SlopeLayer
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceType
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerType
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapLayer
import com.kylecorry.trail_sense.tools.map.map_layers.MyElevationLayer
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationLayer
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.navigation.map_layers.CompassOverlayLayer
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
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
            mapLayers = listOf(
                MapLayerDefinition(
                    BaseMapLayer.LAYER_ID,
                    context.getString(R.string.basemap),
                    layerType = MapLayerType.Tile,
                    description = context.getString(R.string.map_layer_base_map_description)
                ) { BaseMapLayer() },
                MapLayerDefinition(
                    ElevationLayer.LAYER_ID,
                    context.getString(R.string.elevation),
                    layerType = MapLayerType.Tile,
                    description = context.getString(R.string.map_layer_elevation_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = "dem_settings",
                            title = context.getString(R.string.plugin_digital_elevation_model),
                            type = MapLayerPreferenceType.Label,
                            summary = context.getString(R.string.open_settings),
                            openDemSettingsOnClick = true
                        ),
                        MapLayerPreference(
                            id = ElevationLayer.COLOR,
                            title = context.getString(R.string.color),
                            type = MapLayerPreferenceType.Enum,
                            values = listOf(
                                context.getString(R.string.color_usgs) to ElevationColorStrategy.USGS.id.toString(),
                                context.getString(R.string.color_grayscale) to ElevationColorStrategy.Grayscale.id.toString(),
                                context.getString(R.string.color_muted) to ElevationColorStrategy.Muted.id.toString(),
                                context.getString(R.string.color_vibrant) to ElevationColorStrategy.Vibrant.id.toString(),
                                context.getString(R.string.color_viridis) to ElevationColorStrategy.Viridis.id.toString(),
                                context.getString(R.string.color_inferno) to ElevationColorStrategy.Inferno.id.toString(),
                                context.getString(R.string.color_plasma) to ElevationColorStrategy.Plasma.id.toString(),
                            ),
                            defaultValue = ElevationLayer.DEFAULT_COLOR.id.toString(),
                        ),
                        MapLayerPreference(
                            id = ElevationLayer.HIGH_RESOLUTION,
                            title = context.getString(R.string.high_resolution),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = ElevationLayer.DEFAULT_HIGH_RESOLUTION,
                        ),
                    )
                ) { ElevationLayer() },
                MapLayerDefinition(
                    HillshadeLayer.LAYER_ID,
                    context.getString(R.string.hillshade),
                    layerType = MapLayerType.Tile,
                    description = context.getString(R.string.map_layer_hillshade_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = "dem_settings",
                            title = context.getString(R.string.plugin_digital_elevation_model),
                            type = MapLayerPreferenceType.Label,
                            summary = context.getString(R.string.open_settings),
                            openDemSettingsOnClick = true
                        ),
                        MapLayerPreference(
                            id = HillshadeLayer.DRAW_ACCURATE_SHADOWS,
                            title = context.getString(R.string.draw_accurate_shadows),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = HillshadeLayer.DEFAULT_DRAW_ACCURATE_SHADOWS,
                        ),
                        MapLayerPreference(
                            id = HillshadeLayer.HIGH_RESOLUTION,
                            title = context.getString(R.string.high_resolution),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = HillshadeLayer.DEFAULT_HIGH_RESOLUTION,
                        ),
                    )
                ) { HillshadeLayer() },
                MapLayerDefinition(
                    RuggednessLayer.LAYER_ID,
                    context.getString(R.string.ruggedness),
                    layerType = MapLayerType.Tile,
                    description = context.getString(R.string.map_layer_ruggedness_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = "dem_settings",
                            title = context.getString(R.string.plugin_digital_elevation_model),
                            type = MapLayerPreferenceType.Label,
                            summary = context.getString(R.string.open_settings),
                            openDemSettingsOnClick = true
                        ),
                        MapLayerPreference(
                            id = RuggednessLayer.HIGH_RESOLUTION,
                            title = context.getString(R.string.high_resolution),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = RuggednessLayer.DEFAULT_HIGH_RESOLUTION,
                        ),
                    )
                ) { RuggednessLayer() },
                MapLayerDefinition(
                    SlopeLayer.LAYER_ID,
                    context.getString(R.string.path_slope),
                    layerType = MapLayerType.Tile,
                    description = context.getString(R.string.map_layer_slope_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = "dem_settings",
                            title = context.getString(R.string.plugin_digital_elevation_model),
                            type = MapLayerPreferenceType.Label,
                            summary = context.getString(R.string.open_settings),
                            openDemSettingsOnClick = true
                        ),
                        MapLayerPreference(
                            id = SlopeLayer.COLOR,
                            title = context.getString(R.string.color),
                            type = MapLayerPreferenceType.Enum,
                            values = listOf(
                                context.getString(R.string.slope_color_green_to_red) to SlopeColorStrategy.GreenToRed.id.toString(),
                                context.getString(R.string.slope_color_white_to_red) to SlopeColorStrategy.WhiteToRed.id.toString(),
                                context.getString(R.string.color_grayscale) to SlopeColorStrategy.Grayscale.id.toString(),
                            ),
                            defaultValue = SlopeLayer.DEFAULT_COLOR.id.toString(),
                        ),
                        MapLayerPreference(
                            id = SlopeLayer.SMOOTH,
                            title = context.getString(R.string.smooth),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = SlopeLayer.DEFAULT_SMOOTH,
                        ),
                        MapLayerPreference(
                            id = SlopeLayer.HIDE_FLAT_GROUND,
                            title = context.getString(R.string.hide_flat_ground),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = SlopeLayer.DEFAULT_HIDE_FLAT_GROUND,
                        ),
                        MapLayerPreference(
                            id = SlopeLayer.HIGH_RESOLUTION,
                            title = context.getString(R.string.high_resolution),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = SlopeLayer.DEFAULT_HIGH_RESOLUTION,
                        ),
                    )
                ) { SlopeLayer() },
                MapLayerDefinition(
                    AspectLayer.LAYER_ID,
                    context.getString(R.string.aspect),
                    layerType = MapLayerType.Tile,
                    description = context.getString(R.string.map_layer_aspect_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = "dem_settings",
                            title = context.getString(R.string.plugin_digital_elevation_model),
                            type = MapLayerPreferenceType.Label,
                            summary = context.getString(R.string.open_settings),
                            openDemSettingsOnClick = true
                        ),
                        MapLayerPreference(
                            id = AspectLayer.HIGH_RESOLUTION,
                            title = context.getString(R.string.high_resolution),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = AspectLayer.DEFAULT_HIGH_RESOLUTION,
                        ),
                    )
                ) { AspectLayer() },
                MapLayerDefinition(
                    ContourLayer.LAYER_ID,
                    context.getString(R.string.contours),
                    description = context.getString(R.string.map_layer_contours_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = "dem_settings",
                            title = context.getString(R.string.plugin_digital_elevation_model),
                            type = MapLayerPreferenceType.Label,
                            summary = context.getString(R.string.open_settings),
                            openDemSettingsOnClick = true
                        ),
                        MapLayerPreference(
                            id = ContourLayer.SHOW_LABELS,
                            title = context.getString(R.string.show_labels),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = ContourLayer.DEFAULT_SHOW_LABELS,
                        ),
                        MapLayerPreference(
                            id = ContourLayer.COLOR,
                            title = context.getString(R.string.color),
                            type = MapLayerPreferenceType.Enum,
                            values = listOf(
                                context.getString(R.string.color_black) to ElevationColorStrategy.Black.id.toString(),
                                context.getString(R.string.color_brown) to ElevationColorStrategy.Brown.id.toString(),
                                context.getString(R.string.color_gray) to ElevationColorStrategy.Gray.id.toString(),
                                context.getString(R.string.color_white) to ElevationColorStrategy.White.id.toString(),
                                context.getString(R.string.color_usgs) to ElevationColorStrategy.USGS.id.toString(),
                                context.getString(R.string.color_grayscale) to ElevationColorStrategy.Grayscale.id.toString(),
                                context.getString(R.string.color_muted) to ElevationColorStrategy.Muted.id.toString(),
                                context.getString(R.string.color_vibrant) to ElevationColorStrategy.Vibrant.id.toString(),
                                context.getString(R.string.color_viridis) to ElevationColorStrategy.Viridis.id.toString(),
                                context.getString(R.string.color_inferno) to ElevationColorStrategy.Inferno.id.toString(),
                                context.getString(R.string.color_plasma) to ElevationColorStrategy.Plasma.id.toString(),
                            ),
                            defaultValue = ContourLayer.DEFAULT_COLOR.id.toString(),
                        )
                    )
                ) { ContourLayer() },
                MapLayerDefinition(
                    MyLocationLayer.LAYER_ID,
                    context.getString(R.string.location),
                    description = context.getString(R.string.map_layer_my_location_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = MyLocationLayer.SHOW_ACCURACY,
                            title = context.getString(R.string.show_gps_accuracy),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = true,
                        )
                    )
                ) { MyLocationLayer() },
                MapLayerDefinition(
                    ScaleBarLayer.LAYER_ID,
                    context.getString(R.string.map_scale_title),
                    isConfigurable = false,
                    layerType = MapLayerType.Overlay
                ) { ScaleBarLayer() },
                MapLayerDefinition(
                    CompassOverlayLayer.LAYER_ID,
                    context.getString(R.string.pref_compass_sensor_title),
                    isConfigurable = false,
                    layerType = MapLayerType.Overlay
                ) { CompassOverlayLayer() },
                MapLayerDefinition(
                    MyElevationLayer.LAYER_ID,
                    context.getString(R.string.my_elevation),
                    isConfigurable = false,
                    layerType = MapLayerType.Overlay,
                    description = context.getString(R.string.map_layer_my_elevation_description)
                ) { MyElevationLayer() }
            )
        )
    }

    val MAP_ID = "map"
}