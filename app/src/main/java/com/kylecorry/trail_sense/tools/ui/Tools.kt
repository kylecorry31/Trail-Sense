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
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem

data class Tool(
    override val id: Long,
    val name: String,
    @DrawableRes val icon: Int,
    @IdRes val navAction: Int,
    val category: ToolCategory,
    val description: String? = null,
    val guideId: Int? = null,
    val isExperimental: Boolean = false,
    @IdRes val settingsNavAction: Int? = null
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
        val hasFlashlight = FlashlightSubsystem.getInstance(context).isAvailable()
        val prefs = UserPreferences(context)

        return listOfNotNull(
            Tool(
                FLASHLIGHT,
                context.getString(R.string.flashlight_title),
                R.drawable.flashlight,
                R.id.fragmentToolFlashlight,
                ToolCategory.Signaling,
                guideId = R.raw.guide_tool_flashlight,
                // The only settings available are for the physical flashlight
                settingsNavAction = if (hasFlashlight) R.id.flashlightSettingsFragment else null
            ),
            Tool(
                WHISTLE,
                context.getString(R.string.tool_whistle_title),
                R.drawable.ic_tool_whistle,
                R.id.toolWhistleFragment,
                ToolCategory.Signaling,
                guideId = R.raw.guide_tool_whistle
            ),
            Tool(
                RULER,
                context.getString(R.string.tool_ruler_title),
                R.drawable.ruler,
                R.id.rulerFragment,
                ToolCategory.Distance,
                guideId = R.raw.guide_tool_ruler
            ),
            if (hasPedometer) Tool(
                PEDOMETER,
                context.getString(R.string.pedometer),
                R.drawable.steps,
                R.id.fragmentToolPedometer,
                ToolCategory.Distance,
                guideId = R.raw.guide_tool_pedometer,
                settingsNavAction = R.id.calibrateOdometerFragment
            ) else null,
            if (prefs.isCliffHeightEnabled) Tool(
                CLIFF_HEIGHT,
                context.getString(R.string.tool_cliff_height_title),
                R.drawable.ic_tool_cliff_height,
                R.id.toolCliffHeightFragment,
                ToolCategory.Distance,
                context.getString(R.string.tool_cliff_height_description),
                isExperimental = true,
                guideId = R.raw.guide_tool_cliff_height
            ) else null,
            Tool(
                NAVIGATION,
                context.getString(R.string.navigation),
                R.drawable.ic_compass_icon,
                R.id.action_navigation,
                ToolCategory.Location,
                guideId = R.raw.guide_tool_navigation,
                settingsNavAction = R.id.navigationSettingsFragment
            ),
            Tool(
                BEACONS,
                context.getString(R.string.beacons),
                R.drawable.ic_location,
                R.id.beacon_list,
                ToolCategory.Location,
                guideId = R.raw.guide_tool_beacons
            ),
            Tool(
                PHOTO_MAPS,
                context.getString(R.string.photo_maps),
                R.drawable.maps,
                R.id.mapListFragment,
                ToolCategory.Location,
                context.getString(R.string.photo_map_summary),
                guideId = R.raw.guide_tool_photo_maps,
                settingsNavAction = R.id.mapSettingsFragment
            ),
            Tool(
                PATHS,
                context.getString(R.string.paths),
                R.drawable.ic_tool_backtrack,
                R.id.fragmentBacktrack,
                ToolCategory.Location,
                guideId = R.raw.guide_tool_paths,
                settingsNavAction = R.id.pathsSettingsFragment
            ),
            Tool(
                TRIANGULATE_LOCATION,
                context.getString(R.string.tool_triangulate_title),
                R.drawable.ic_tool_triangulate,
                R.id.fragmentToolTriangulate,
                ToolCategory.Location,
                guideId = R.raw.guide_tool_triangulate_location
            ),
            Tool(
                CLINOMETER,
                context.getString(R.string.clinometer_title),
                R.drawable.clinometer,
                R.id.clinometerFragment,
                ToolCategory.Angles,
                context.getString(R.string.tool_clinometer_summary),
                guideId = R.raw.guide_tool_clinometer,
                settingsNavAction = R.id.clinometerSettingsFragment
            ),
            Tool(
                BUBBLE_LEVEL,
                context.getString(R.string.tool_bubble_level_title),
                R.drawable.level,
                R.id.levelFragment,
                ToolCategory.Angles,
                context.getString(R.string.tool_bubble_level_summary),
                guideId = R.raw.guide_tool_bubble_level
            ),
            Tool(
                CLOCK,
                context.getString(R.string.tool_clock_title),
                R.drawable.ic_tool_clock,
                R.id.toolClockFragment,
                ToolCategory.Time,
                guideId = R.raw.guide_tool_clock
            ),
            Tool(
                ASTRONOMY,
                context.getString(R.string.astronomy),
                R.drawable.ic_astronomy,
                R.id.action_astronomy,
                ToolCategory.Time,
                guideId = R.raw.guide_tool_astronomy,
                settingsNavAction = R.id.astronomySettingsFragment
            ),
            Tool(
                WATER_BOIL_TIMER,
                context.getString(R.string.water_boil_timer),
                R.drawable.ic_tool_boil_done,
                R.id.waterPurificationFragment,
                ToolCategory.Time,
                context.getString(R.string.tool_boil_summary),
                guideId = R.raw.guide_tool_water_boil_timer
            ),
            Tool(
                TIDES,
                context.getString(R.string.tides),
                R.drawable.ic_tide_table,
                R.id.tidesFragment,
                ToolCategory.Time,
                guideId = R.raw.guide_tool_tides,
                settingsNavAction = R.id.tideSettingsFragment
            ),
            Tool(
                BATTERY,
                context.getString(R.string.tool_battery_title),
                R.drawable.ic_tool_battery,
                R.id.fragmentToolBattery,
                ToolCategory.Power,
                guideId = R.raw.guide_tool_battery,
                settingsNavAction = R.id.powerSettingsFragment
            ),
            if (hasCompass) Tool(
                SOLAR_PANEL_ALIGNER,
                context.getString(R.string.tool_solar_panel_title),
                R.drawable.ic_tool_solar_panel,
                R.id.fragmentToolSolarPanel,
                ToolCategory.Power,
                context.getString(R.string.tool_solar_panel_summary),
                guideId = R.raw.guide_tool_solar_panel_aligner
            ) else null,
            if (hasLightMeter) Tool(
                LIGHT_METER,
                context.getString(R.string.tool_light_meter_title),
                R.drawable.flashlight,
                R.id.toolLightFragment,
                ToolCategory.Power,
                context.getString(R.string.guide_light_meter_description),
                guideId = R.raw.guide_tool_light_meter
            ) else null,
            if (hasBarometer) Tool(
                WEATHER,
                context.getString(R.string.weather),
                R.drawable.cloud,
                R.id.action_weather,
                ToolCategory.Weather,
                guideId = R.raw.guide_tool_weather,
                settingsNavAction = R.id.weatherSettingsFragment
            ) else null,
            Tool(
                CLIMATE,
                context.getString(R.string.tool_climate),
                R.drawable.ic_temperature_range,
                R.id.climateFragment,
                ToolCategory.Weather,
                context.getString(R.string.tool_climate_summary),
                guideId = R.raw.guide_tool_climate
            ),
            Tool(
                TEMPERATURE_ESTIMATION,
                context.getString(R.string.tool_temperature_estimation_title),
                R.drawable.thermometer,
                R.id.temperatureEstimationFragment,
                ToolCategory.Weather,
                context.getString(R.string.tool_temperature_estimation_description),
                guideId = R.raw.guide_tool_temperature_estimation
            ),
            Tool(
                CLOUDS,
                context.getString(R.string.clouds),
                R.drawable.ic_tool_clouds,
                R.id.cloudFragment,
                ToolCategory.Weather,
                guideId = R.raw.guide_tool_clouds
            ),
            Tool(
                LIGHTNING_STRIKE_DISTANCE,
                context.getString(R.string.tool_lightning_title),
                R.drawable.ic_torch_on,
                R.id.fragmentToolLightning,
                ToolCategory.Weather,
                context.getString(R.string.tool_lightning_description),
                guideId = R.raw.guide_tool_lightning_strike_distance
            ),
            if (prefs.isAugmentedRealityEnabled && hasCompass) Tool(
                AUGMENTED_REALITY,
                context.getString(R.string.augmented_reality),
                R.drawable.ic_camera,
                R.id.augmentedRealityFragment,
                ToolCategory.Other,
                context.getString(R.string.augmented_reality_description),
                isExperimental = true,
                guideId = R.raw.guide_tool_augmented_reality,
                settingsNavAction = R.id.augmentedRealitySettingsFragment
            ) else null,
            Tool(
                CONVERT,
                context.getString(R.string.convert),
                R.drawable.ic_tool_distance_convert,
                R.id.toolConvertFragment,
                ToolCategory.Other,
                guideId = R.raw.guide_tool_convert
            ),
            Tool(
                PACKING_LISTS,
                context.getString(R.string.packing_lists),
                R.drawable.ic_tool_pack,
                R.id.packListFragment,
                ToolCategory.Other,
                guideId = R.raw.guide_tool_packing_lists
            ),
            if (hasCompass) Tool(
                METAL_DETECTOR,
                context.getString(R.string.tool_metal_detector_title),
                R.drawable.ic_tool_metal_detector,
                R.id.fragmentToolMetalDetector,
                ToolCategory.Other,
                guideId = R.raw.guide_tool_metal_detector
            ) else null,
            Tool(
                WHITE_NOISE,
                context.getString(R.string.tool_white_noise_title),
                R.drawable.ic_tool_white_noise,
                R.id.fragmentToolWhiteNoise,
                ToolCategory.Other,
                context.getString(R.string.tool_white_noise_summary),
                guideId = R.raw.guide_tool_white_noise
            ),
            Tool(
                NOTES,
                context.getString(R.string.tool_notes_title),
                R.drawable.ic_tool_notes,
                R.id.fragmentToolNotes,
                ToolCategory.Other,
                guideId = R.raw.guide_tool_notes
            ),
            Tool(
                QR_CODE_SCANNER,
                context.getString(R.string.qr_code_scanner),
                R.drawable.ic_qr_code,
                R.id.scanQrFragment,
                ToolCategory.Other,
                guideId = R.raw.guide_tool_qr_code_scanner
            ),
            Tool(
                SENSORS,
                context.getString(R.string.sensors),
                R.drawable.ic_sensors,
                R.id.sensorDetailsFragment,
                ToolCategory.Other,
                guideId = R.raw.guide_tool_sensors
            ),
            Tool(
                DIAGNOSTICS,
                context.getString(R.string.diagnostics),
                R.drawable.ic_alert,
                R.id.diagnosticsFragment,
                ToolCategory.Other,
                guideId = R.raw.guide_tool_diagnostics
            ),
            Tool(
                SETTINGS,
                context.getString(R.string.settings),
                R.drawable.ic_settings,
                R.id.action_settings,
                ToolCategory.Other,
            ),
            Tool(
                USER_GUIDE,
                context.getString(R.string.tool_user_guide_title),
                R.drawable.ic_user_guide,
                R.id.guideListFragment,
                ToolCategory.Other,
                context.getString(R.string.tool_user_guide_summary)
            ),
            if (isDebug()) Tool(
                EXPERIMENTATION,
                "Experimentation",
                R.drawable.ic_experimental,
                R.id.experimentationFragment,
                ToolCategory.Other,
                isExperimental = true
            ) else null
        )
    }

    // Tool IDs
    const val FLASHLIGHT = 1L
    const val WHISTLE = 2L
    const val RULER = 3L
    const val PEDOMETER = 4L
    const val CLIFF_HEIGHT = 5L
    const val NAVIGATION = 6L
    const val BEACONS = 7L
    const val PHOTO_MAPS = 8L
    const val PATHS = 9L
    const val TRIANGULATE_LOCATION = 10L
    const val CLINOMETER = 11L
    const val BUBBLE_LEVEL = 12L
    const val CLOCK = 13L
    const val ASTRONOMY = 14L
    const val WATER_BOIL_TIMER = 15L
    const val TIDES = 16L
    const val BATTERY = 17L
    const val SOLAR_PANEL_ALIGNER = 18L
    const val LIGHT_METER = 19L
    const val WEATHER = 20L
    const val CLIMATE = 21L
    const val TEMPERATURE_ESTIMATION = 22L
    const val CLOUDS = 23L
    const val LIGHTNING_STRIKE_DISTANCE = 24L
    const val AUGMENTED_REALITY = 25L
    const val CONVERT = 26L
    const val PACKING_LISTS = 27L
    const val METAL_DETECTOR = 28L
    const val WHITE_NOISE = 29L
    const val NOTES = 30L
    const val QR_CODE_SCANNER = 31L
    const val SENSORS = 32L
    const val DIAGNOSTICS = 33L
    const val SETTINGS = 34L
    const val USER_GUIDE = 35L
    const val EXPERIMENTATION = 36L
}
