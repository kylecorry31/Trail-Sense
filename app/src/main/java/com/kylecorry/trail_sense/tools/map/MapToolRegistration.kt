package com.kylecorry.trail_sense.tools.map

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.dem.colors.ElevationColorStrategy
import com.kylecorry.trail_sense.shared.dem.colors.SlopeColorStrategy
import com.kylecorry.trail_sense.shared.dem.map_layers.AspectLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.AspectMapTileSource
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourGeoJsonSource
import com.kylecorry.trail_sense.shared.dem.map_layers.ContourLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.ElevationLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.ElevationMapTileSource
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.HillshadeMapTileSource
import com.kylecorry.trail_sense.shared.dem.map_layers.RuggednessLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.RuggednessMapTileSource
import com.kylecorry.trail_sense.shared.dem.map_layers.SlopeLayer
import com.kylecorry.trail_sense.shared.dem.map_layers.SlopeMapTileSource
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceType
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerType
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapLayer
import com.kylecorry.trail_sense.tools.map.map_layers.BaseMapTileSource
import com.kylecorry.trail_sense.tools.map.map_layers.MyElevationLayer
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationLayer
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationGeoJsonSource
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.navigation.map_layers.CompassOverlayLayer
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
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
            broadcasts = listOf(
                ToolBroadcast(
                    BROADCAST_GEOJSON_FEATURE_SELECTION_CHANGED,
                    "GeoJSON feature selection changed"
                )
            ),
            mapLayers = listOf(
                MapLayerDefinition(
                    BaseMapTileSource.SOURCE_ID,
                    context.getString(R.string.basemap),
                    layerType = MapLayerType.Tile,
                    description = context.getString(R.string.map_layer_base_map_description)
                ) { BaseMapLayer() },
                MapLayerDefinition(
                    ElevationMapTileSource.SOURCE_ID,
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
                            id = ElevationMapTileSource.COLOR,
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
                            defaultValue = ElevationMapTileSource.DEFAULT_COLOR.id.toString(),
                        ),
                        MapLayerPreference(
                            id = ElevationMapTileSource.HIGH_RESOLUTION,
                            title = context.getString(R.string.high_resolution),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = ElevationMapTileSource.DEFAULT_HIGH_RESOLUTION,
                        ),
                    )
                ) { ElevationLayer() },
                MapLayerDefinition(
                    HillshadeMapTileSource.SOURCE_ID,
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
                            id = HillshadeMapTileSource.DRAW_ACCURATE_SHADOWS,
                            title = context.getString(R.string.draw_accurate_shadows),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = HillshadeMapTileSource.DEFAULT_DRAW_ACCURATE_SHADOWS,
                        ),
                        MapLayerPreference(
                            id = HillshadeMapTileSource.HIGH_RESOLUTION,
                            title = context.getString(R.string.high_resolution),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = HillshadeMapTileSource.DEFAULT_HIGH_RESOLUTION,
                        ),
                        MapLayerPreference(
                            id = HillshadeMapTileSource.MULTI_DIRECTION_SHADING,
                            title = context.getString(R.string.multi_direction_shading),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = HillshadeMapTileSource.DEFAULT_MULTI_DIRECTION_SHADING,
                        ),
                    )
                ) { HillshadeLayer() },
                MapLayerDefinition(
                    RuggednessMapTileSource.SOURCE_ID,
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
                            id = RuggednessMapTileSource.HIGH_RESOLUTION,
                            title = context.getString(R.string.high_resolution),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = RuggednessMapTileSource.DEFAULT_HIGH_RESOLUTION,
                        ),
                    )
                ) { RuggednessLayer() },
                MapLayerDefinition(
                    SlopeMapTileSource.SOURCE_ID,
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
                            id = SlopeMapTileSource.COLOR,
                            title = context.getString(R.string.color),
                            type = MapLayerPreferenceType.Enum,
                            values = listOf(
                                context.getString(R.string.slope_color_green_to_red) to SlopeColorStrategy.GreenToRed.id.toString(),
                                context.getString(R.string.slope_color_white_to_red) to SlopeColorStrategy.WhiteToRed.id.toString(),
                                context.getString(R.string.color_grayscale) to SlopeColorStrategy.Grayscale.id.toString(),
                            ),
                            defaultValue = SlopeMapTileSource.DEFAULT_COLOR.id.toString(),
                        ),
                        MapLayerPreference(
                            id = SlopeMapTileSource.SMOOTH,
                            title = context.getString(R.string.smooth),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = SlopeMapTileSource.DEFAULT_SMOOTH,
                        ),
                        MapLayerPreference(
                            id = SlopeMapTileSource.HIDE_FLAT_GROUND,
                            title = context.getString(R.string.hide_flat_ground),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = SlopeMapTileSource.DEFAULT_HIDE_FLAT_GROUND,
                        ),
                        MapLayerPreference(
                            id = SlopeMapTileSource.HIGH_RESOLUTION,
                            title = context.getString(R.string.high_resolution),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = SlopeMapTileSource.DEFAULT_HIGH_RESOLUTION,
                        ),
                    )
                ) { SlopeLayer() },
                MapLayerDefinition(
                    AspectMapTileSource.SOURCE_ID,
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
                            id = AspectMapTileSource.HIGH_RESOLUTION,
                            title = context.getString(R.string.high_resolution),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = AspectMapTileSource.DEFAULT_HIGH_RESOLUTION,
                        ),
                    )
                ) { AspectLayer() },
                MapLayerDefinition(
                    ContourGeoJsonSource.SOURCE_ID,
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
                            id = ContourGeoJsonSource.SHOW_LABELS,
                            title = context.getString(R.string.show_labels),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = ContourGeoJsonSource.DEFAULT_SHOW_LABELS,
                        ),
                        MapLayerPreference(
                            id = ContourGeoJsonSource.COLOR,
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
                            defaultValue = ContourGeoJsonSource.DEFAULT_COLOR.id.toString(),
                        )
                    )
                ) { ContourLayer() },
                MapLayerDefinition(
                    MyLocationGeoJsonSource.SOURCE_ID,
                    context.getString(R.string.location),
                    description = context.getString(R.string.map_layer_my_location_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = MyLocationGeoJsonSource.SHOW_ACCURACY,
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

    const val BROADCAST_GEOJSON_FEATURE_SELECTION_CHANGED =
        "map-broadcast-geojson-feature-selection-changed"
    const val BROADCAST_PARAM_GEOJSON_FEATURE_ID = "featureId"
    const val BROADCAST_PARAM_GEOJSON_LAYER_ID = "layerId"
}
