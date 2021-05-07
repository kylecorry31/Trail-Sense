package com.kylecorry.trail_sense.tools.ui

import android.content.Context
import android.hardware.Sensor
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker

data class ToolGroup(val name: String, val tools: List<Tool>)
data class Tool(
    val name: String,
    @DrawableRes val icon: Int,
    @IdRes val navAction: Int,
    val description: String? = null
)

object Tools {
    fun getTools(context: Context): List<ToolGroup> {
        val prefs = UserPreferences(context)
        val sensorChecker = SensorChecker(context)
        val experimental = prefs.experimentalEnabled
        val hasLightMeter = sensorChecker.hasSensor(Sensor.TYPE_LIGHT)
        val hasBarometer = sensorChecker.hasBarometer()
        val signaling = ToolGroup(
            context.getString(R.string.tool_category_signaling), listOf(
                Tool(
                    context.getString(R.string.flashlight_title),
                    R.drawable.flashlight,
                    R.id.action_action_experimental_tools_to_fragmentToolFlashlight,
                ),
                Tool(
                    context.getString(R.string.tool_whistle_title),
                    R.drawable.ic_tool_whistle,
                    R.id.action_action_experimental_tools_to_toolWhistleFragment,
                )
            )
        )
        val distance = ToolGroup(
            context.getString(R.string.tool_category_distance), listOfNotNull(
                Tool(
                    context.getString(R.string.tool_ruler_title),
                    R.drawable.ruler,
                    R.id.action_action_experimental_tools_to_rulerFragment
                ),
                Tool(
                    context.getString(R.string.tool_speedometer_odometer_title),
                    R.drawable.ic_tool_speedometer,
                    R.id.action_toolsFragment_to_speedometerFragment
                ),
                Tool(
                    context.getString(R.string.tool_distance_convert_title),
                    R.drawable.ic_tool_distance_convert,
                    R.id.action_action_experimental_tools_to_fragmentDistanceConverter
                ),
                Tool(
                    context.getString(R.string.tool_cliff_height_title),
                    R.drawable.ic_tool_cliff_height,
                    R.id.action_action_experimental_tools_to_toolCliffHeightFragment,
                    context.getString(R.string.tool_cliff_height_description)
                ),
                if (hasBarometer) Tool(
                    context.getString(R.string.tool_depth_title),
                    R.drawable.ic_depth,
                    R.id.action_action_experimental_tools_to_toolDepthFragment,
                    context.getString(R.string.tool_depth_summary)
                ) else null
            )
        )

        val location = ToolGroup(
            context.getString(R.string.tool_category_location), listOfNotNull(
                if (experimental) Tool(
                    context.getString(R.string.offline_maps),
                    R.drawable.maps,
                    R.id.action_tools_to_maps_list,
                    context.getString(R.string.experimental)
                ) else null,
                Tool(
                    context.getString(R.string.tool_backtrack_title),
                    R.drawable.ic_tool_backtrack,
                    R.id.action_action_experimental_tools_to_fragmentBacktrack,
                    context.getString(R.string.tool_backtrack_summary)
                ),
                Tool(
                    context.getString(R.string.tool_triangulate_title),
                    R.drawable.ic_tool_triangulate,
                    R.id.action_action_experimental_tools_to_fragmentToolTriangulate,
                    context.getString(R.string.tool_triangulate_summary)
                ),
                Tool(
                    context.getString(R.string.tool_coordinate_convert_title),
                    R.drawable.ic_tool_distance_convert,
                    R.id.action_action_experimental_tools_to_fragmentToolCoordinateConvert
                )
            )
        )

        val angles = ToolGroup(
            context.getString(R.string.tool_category_angles), listOfNotNull(
                Tool(
                    context.getString(R.string.inclinometer_title),
                    R.drawable.inclinometer,
                    R.id.action_toolsFragment_to_inclinometerFragment,
                    context.getString(R.string.tool_inclinometer_summary)
                ),
                Tool(
                    context.getString(R.string.tool_bubble_level_title),
                    R.drawable.level,
                    R.id.action_action_experimental_tools_to_levelFragment,
                    context.getString(R.string.tool_bubble_level_summary)
                )
            )
        )

        val time = ToolGroup(
            context.getString(R.string.tool_category_time), listOfNotNull(
                Tool(
                    context.getString(R.string.tool_clock_title),
                    R.drawable.ic_tool_clock,
                    R.id.action_action_experimental_tools_to_toolClockFragment
                ),
                Tool(
                    context.getString(R.string.tool_boil_title),
                    R.drawable.ic_tool_boil,
                    R.id.action_action_experimental_tools_to_waterPurificationFragment,
                    context.getString(R.string.tool_boil_summary)
                ),
                if (experimental) Tool(
                    context.getString(R.string.tides),
                    R.drawable.ic_tool_tides,
                    R.id.action_toolsFragment_to_tidesFragment,
                    context.getString(R.string.experimental)
                ) else null
            )
        )

        val power = ToolGroup(
            context.getString(R.string.tool_category_power), listOfNotNull(
                Tool(
                    context.getString(R.string.tool_battery_title),
                    R.drawable.ic_tool_battery,
                    R.id.action_action_experimental_tools_to_fragmentToolBattery
                ),
                Tool(
                    context.getString(R.string.tool_solar_panel_title),
                    R.drawable.ic_tool_solar_panel,
                    R.id.action_action_experimental_tools_to_fragmentToolSolarPanel,
                    context.getString(R.string.tool_solar_panel_summary)
                ),
                if (hasLightMeter) Tool(
                    context.getString(R.string.tool_light_meter_title),
                    R.drawable.flashlight,
                    R.id.action_toolsFragment_to_toolLightFragment,
                    context.getString(R.string.guide_light_meter_description)
                ) else null
            )
        )

        val weather = ToolGroup(
            context.getString(R.string.weather), listOfNotNull(
                Tool(
                    context.getString(R.string.tool_thermometer_title),
                    R.drawable.thermometer,
                    R.id.action_action_experimental_tools_to_thermometerFragment
                ),
                Tool(
                    context.getString(R.string.clouds),
                    R.drawable.ic_tool_clouds,
                    R.id.action_action_experimental_tools_to_cloudFragment
                ),
                Tool(
                    context.getString(R.string.tool_lightning_title),
                    R.drawable.ic_tool_lightning,
                    R.id.action_action_experimental_tools_to_fragmentToolLightning,
                    context.getString(R.string.tool_lightning_description)
                )
            )
        )

        val other = ToolGroup(
            context.getString(R.string.tool_category_other), listOfNotNull(
                Tool(
                    context.getString(R.string.convert),
                    R.drawable.ic_tool_distance_convert,
                    R.id.action_toolsFragment_to_toolConvertFragment
                ),
                Tool(
                    context.getString(R.string.tool_metal_detector_title),
                    R.drawable.ic_tool_metal_detector,
                    R.id.action_action_experimental_tools_to_fragmentToolMetalDetector
                ),
                Tool(
                    context.getString(R.string.tool_white_noise_title),
                    R.drawable.ic_tool_white_noise,
                    R.id.action_action_experimental_tools_to_fragmentToolWhiteNoise,
                    context.getString(R.string.tool_white_noise_summary)
                ),
                Tool(
                    context.getString(R.string.tool_notes_title),
                    R.drawable.ic_tool_notes,
                    R.id.action_action_experimental_tools_to_fragmentToolNotes
                ),
                Tool(
                    context.getString(R.string.action_inventory),
                    R.drawable.ic_inventory,
                    R.id.action_action_experimental_tools_to_action_inventory,
                    context.getString(R.string.tool_inventory_summary)
                ),
                Tool(
                    context.getString(R.string.tool_user_guide_title),
                    R.drawable.ic_user_guide,
                    R.id.action_action_experimental_tools_to_guideListFragment,
                    context.getString(R.string.tool_user_guide_summary)
                )
            )
        )

        return listOf(
            signaling,
            distance,
            location,
            angles,
            time,
            power,
            weather,
            other
        )

    }
}
