package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.content.Context
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory

object Guides {

    fun guides(context: Context): List<UserGuideCategory> {

        val general = UserGuideCategory(
            context.getString(R.string.general), listOf(
                UserGuide(
                    context.getString(R.string.guide_conserving_battery_title),
                    null,
                    R.raw.conserving_battery
                ),
                UserGuide(
                    context.getString(R.string.guide_signaling_for_help_title),
                    null,
                    R.raw.signaling_for_help
                )
            )
        )

        val navigation = UserGuideCategory(
            context.getString(R.string.navigation), listOf(
                UserGuide(
                    context.getString(R.string.navigation),
                    null,
                    R.raw.navigate
                ),
                UserGuide(
                    context.getString(R.string.guide_using_printed_maps),
                    null,
                    R.raw.using_printed_maps
                ),
                UserGuide(
                    context.getString(R.string.guide_importing_maps_title),
                    context.getString(R.string.experimental),
                    R.raw.importing_maps
                ),
                UserGuide(
                    context.getString(R.string.guide_location_no_gps_title),
                    null,
                    R.raw.determine_location_without_gps
                ),
                UserGuide(
                    context.getString(R.string.guide_speedometer_title),
                    context.getString(R.string.guide_speedometer_desc),
                    R.raw.speed_distance
                )
            )
        )

        val weather = UserGuideCategory(
            context.getString(R.string.weather), listOfNotNull(
                if (Sensors.hasBarometer(context))
                    UserGuide(
                        context.getString(R.string.guide_weather_prediction_title),
                        null,
                        R.raw.weather
                    ) else null,
                if (Sensors.hasBarometer(context)) UserGuide(
                    context.getString(R.string.guide_barometer_calibration_title),
                    null,
                    R.raw.calibrating_barometer
                ) else null,
                UserGuide(
                    context.getString(R.string.guide_thermometer_calibration_title),
                    null,
                    R.raw.calibrating_thermometer
                )
            )
        )

        val tools = UserGuideCategory(
            context.getString(R.string.tools), listOf(
                UserGuide(
                    context.getString(R.string.guide_packing_list),
                    null,
                    R.raw.packing_lists
                ),
                UserGuide(
                    context.getString(R.string.guide_avalanche_risk),
                    null,
                    R.raw.determine_avalanche_risk
                ),
                UserGuide(
                    context.getString(R.string.object_height_guide),
                    null,
                    R.raw.height_estimation
                ),
                UserGuide(
                    context.getString(R.string.cliff_height_guide),
                    null,
                    R.raw.cliff_height
                ),
                UserGuide(
                    context.getString(R.string.guide_light_meter_title),
                    context.getString(R.string.guide_light_meter_description),
                    R.raw.flashlight_testing
                ),
                UserGuide(
                    context.getString(R.string.water_boil_guide_title),
                    null,
                    R.raw.making_water_potable
                ),
                UserGuide(
                    context.getString(R.string.guide_recommended_apps),
                    context.getString(R.string.guide_recommended_apps_description),
                    R.raw.recommended_apps
                )
            )
        )

        return listOf(
            general,
            navigation,
            weather,
            tools
        )
    }
}