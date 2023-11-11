package com.kylecorry.trail_sense.tools.ui

import android.content.Context
import android.hardware.Sensor
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.database.Identifiable
import com.kylecorry.trail_sense.shared.extensions.isDebug
import com.kylecorry.trail_sense.shared.sensors.SensorService

data class Tool(
    override val id: Long,
    val name: String,
    @DrawableRes val icon: Int,
    @IdRes val navAction: Int,
    val category: ToolCategory,
    val description: String? = null,
    val guideId: Int? = null,
    val isExperimental: Boolean = false
) : Identifiable

enum class ToolCategory {
    Signaling,
    Distance,
    Location,
    Angles,
    Time,
    Power,
    Weather,
    Other
}

object Tools {
    fun getTools(context: Context): List<Tool> {
        val hasLightMeter = Sensors.hasSensor(context, Sensor.TYPE_LIGHT)
        val hasPedometer = Sensors.hasSensor(context, Sensor.TYPE_STEP_COUNTER)
        val hasCompass = SensorService(context).hasCompass()
        val hasBarometer = Sensors.hasBarometer(context)
        val prefs = UserPreferences(context)

        return listOfNotNull(
            Tool(
                1,
                context.getString(R.string.flashlight_title),
                R.drawable.flashlight,
                R.id.action_action_experimental_tools_to_fragmentToolFlashlight,
                ToolCategory.Signaling,
            ),
            Tool(
                2,
                context.getString(R.string.tool_whistle_title),
                R.drawable.ic_tool_whistle,
                R.id.action_action_experimental_tools_to_toolWhistleFragment,
                ToolCategory.Signaling,
            ),
            Tool(
                3,
                context.getString(R.string.tool_ruler_title),
                R.drawable.ruler,
                R.id.action_action_experimental_tools_to_rulerFragment,
                ToolCategory.Distance,
            ),
            if (hasPedometer) Tool(
                4,
                context.getString(R.string.pedometer),
                R.drawable.steps,
                R.id.action_tools_to_pedometer,
                ToolCategory.Distance,
            ) else null,
            if (prefs.isCliffHeightEnabled) Tool(
                5,
                context.getString(R.string.tool_cliff_height_title),
                R.drawable.ic_tool_cliff_height,
                R.id.action_action_experimental_tools_to_toolCliffHeightFragment,
                ToolCategory.Distance,
                context.getString(R.string.experimental) + " - " + context.getString(R.string.tool_cliff_height_description),
                isExperimental = true,
                guideId = R.raw.cliff_height
            ) else null,
            Tool(
                6,
                context.getString(R.string.navigation),
                R.drawable.ic_compass_icon,
                R.id.action_navigation,
                ToolCategory.Location,
                guideId = R.raw.navigate
            ),
            Tool(
                7,
                context.getString(R.string.beacons),
                R.drawable.ic_location,
                R.id.action_tools_to_beacons,
                ToolCategory.Location,
                guideId = R.raw.navigate
            ),
            Tool(
                8,
                context.getString(R.string.photo_maps),
                R.drawable.maps,
                R.id.action_tools_to_maps_list,
                ToolCategory.Location,
                context.getString(R.string.photo_map_summary),
                guideId = R.raw.importing_maps
            ),
            Tool(
                9,
                context.getString(R.string.paths),
                R.drawable.ic_tool_backtrack,
                R.id.action_action_experimental_tools_to_fragmentBacktrack,
                ToolCategory.Location,
                guideId = R.raw.navigate
            ),
            Tool(
                10,
                context.getString(R.string.tool_triangulate_title),
                R.drawable.ic_tool_triangulate,
                R.id.action_action_experimental_tools_to_fragmentToolTriangulate,
                ToolCategory.Location,
                guideId = R.raw.determine_location_without_gps
            ),
            Tool(
                11,
                context.getString(R.string.clinometer_title),
                R.drawable.clinometer,
                R.id.action_toolsFragment_to_clinometerFragment,
                ToolCategory.Angles,
                context.getString(R.string.tool_clinometer_summary),
                guideId = R.raw.clinometer
            ),
            Tool(
                12,
                context.getString(R.string.tool_bubble_level_title),
                R.drawable.level,
                R.id.action_action_experimental_tools_to_levelFragment,
                ToolCategory.Angles,
                context.getString(R.string.tool_bubble_level_summary)
            ),
            Tool(
                13,
                context.getString(R.string.tool_clock_title),
                R.drawable.ic_tool_clock,
                R.id.action_action_experimental_tools_to_toolClockFragment,
                ToolCategory.Time,
            ),
            Tool(
                14,
                context.getString(R.string.astronomy),
                R.drawable.ic_astronomy,
                R.id.action_astronomy,
                ToolCategory.Time
            ),
            Tool(
                15,
                context.getString(R.string.water_boil_timer),
                R.drawable.ic_tool_boil,
                R.id.action_action_experimental_tools_to_waterPurificationFragment,
                ToolCategory.Time,
                context.getString(R.string.tool_boil_summary),
                guideId = R.raw.making_water_potable
            ),
            Tool(
                16,
                context.getString(R.string.tides),
                R.drawable.ic_tide_table,
                R.id.action_toolsFragment_to_tidesFragment,
                ToolCategory.Time,
                guideId = R.raw.tides
            ),
            Tool(
                17,
                context.getString(R.string.tool_battery_title),
                R.drawable.ic_tool_battery,
                R.id.action_action_experimental_tools_to_fragmentToolBattery,
                ToolCategory.Power,
                guideId = R.raw.conserving_battery
            ),
            if (hasCompass) Tool(
                18,
                context.getString(R.string.tool_solar_panel_title),
                R.drawable.ic_tool_solar_panel,
                R.id.action_action_experimental_tools_to_fragmentToolSolarPanel,
                ToolCategory.Power,
                context.getString(R.string.tool_solar_panel_summary)
            ) else null,
            if (hasLightMeter) Tool(
                19,
                context.getString(R.string.tool_light_meter_title),
                R.drawable.flashlight,
                R.id.action_toolsFragment_to_toolLightFragment,
                ToolCategory.Power,
                context.getString(R.string.guide_light_meter_description),
                guideId = R.raw.flashlight_testing
            ) else null,
            if (hasBarometer) Tool(
                20,
                context.getString(R.string.weather),
                R.drawable.cloud,
                R.id.action_weather,
                ToolCategory.Weather,
                guideId = R.raw.weather
            ) else null,
            Tool(
                21,
                context.getString(R.string.tool_climate),
                R.drawable.ic_temperature_range,
                R.id.action_toolsFragment_to_toolClimate,
                ToolCategory.Weather,
                context.getString(R.string.tool_climate_summary),
                guideId = R.raw.weather
            ),
            Tool(
                22,
                context.getString(R.string.tool_temperature_estimation_title),
                R.drawable.thermometer,
                R.id.action_tools_to_temperature_estimation,
                ToolCategory.Weather,
                context.getString(R.string.tool_temperature_estimation_description),
                guideId = R.raw.weather
            ),
            Tool(
                23,
                context.getString(R.string.clouds),
                R.drawable.ic_tool_clouds,
                R.id.action_action_experimental_tools_to_cloudFragment,
                ToolCategory.Weather,
                guideId = R.raw.weather
            ),
            Tool(
                24,
                context.getString(R.string.tool_lightning_title),
                R.drawable.ic_torch_on,
                R.id.action_action_experimental_tools_to_fragmentToolLightning,
                ToolCategory.Weather,
                context.getString(R.string.tool_lightning_description)
            ),
            if (prefs.isAugmentedRealityEnabled && hasCompass) Tool(
                25,
                context.getString(R.string.augmented_reality),
                R.drawable.ic_camera,
                R.id.action_tools_to_augmented_reality,
                ToolCategory.Other,
                context.getString(R.string.augmented_reality_description)
            ) else null,
            Tool(
                26,
                context.getString(R.string.convert),
                R.drawable.ic_tool_distance_convert,
                R.id.action_toolsFragment_to_toolConvertFragment,
                ToolCategory.Other,
            ),
            Tool(
                27,
                context.getString(R.string.packing_lists),
                R.drawable.ic_tool_pack,
                R.id.action_action_experimental_tools_to_action_inventory,
                ToolCategory.Other,
                guideId = R.raw.packing_lists
            ),
            if (hasCompass) Tool(
                28,
                context.getString(R.string.tool_metal_detector_title),
                R.drawable.ic_tool_metal_detector,
                R.id.action_action_experimental_tools_to_fragmentToolMetalDetector,
                ToolCategory.Other,
            ) else null,
            Tool(
                29,
                context.getString(R.string.tool_white_noise_title),
                R.drawable.ic_tool_white_noise,
                R.id.action_action_experimental_tools_to_fragmentToolWhiteNoise,
                ToolCategory.Other,
                context.getString(R.string.tool_white_noise_summary)
            ),
            Tool(
                30,
                context.getString(R.string.tool_notes_title),
                R.drawable.ic_tool_notes,
                R.id.action_action_experimental_tools_to_fragmentToolNotes,
                ToolCategory.Other,
            ),
            Tool(
                31,
                context.getString(R.string.qr_code_scanner),
                R.drawable.ic_qr_code,
                R.id.action_tools_to_qr,
                ToolCategory.Other,
            ),
            Tool(
                32,
                context.getString(R.string.sensors),
                R.drawable.ic_sensors,
                R.id.sensorDetailsFragment,
                ToolCategory.Other,
            ),
            Tool(
                33,
                context.getString(R.string.diagnostics),
                R.drawable.ic_alert,
                R.id.action_tools_to_diagnostics,
                ToolCategory.Other,
            ),
            Tool(
                34,
                context.getString(R.string.settings),
                R.drawable.ic_settings,
                R.id.action_settings,
                ToolCategory.Other,
            ),
            Tool(
                35,
                context.getString(R.string.tool_user_guide_title),
                R.drawable.ic_user_guide,
                R.id.action_action_experimental_tools_to_guideListFragment,
                ToolCategory.Other,
                context.getString(R.string.tool_user_guide_summary)
            ),
            if (isDebug()) Tool(
                36,
                "Experimentation",
                R.drawable.ic_experimental,
                R.id.experimentationFragment,
                ToolCategory.Other,
            ) else null
        )
    }
}
