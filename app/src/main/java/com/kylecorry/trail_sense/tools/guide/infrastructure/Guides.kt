package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory

object Guides {

    fun guides(context: Context): List<UserGuideCategory> {

        val general = UserGuideCategory(
            context.getString(R.string.pref_general_header), listOf(
                UserGuide(
                    context.getString(R.string.guide_conserving_battery_title),
                    context.getString(R.string.guide_conserving_battery_description),
                    R.raw.conserving_battery
                ),
                UserGuide(
                    context.getString(R.string.guide_signaling_for_help_title),
                    context.getString(R.string.guide_signaling_for_help_description),
                    R.raw.signaling_for_help
                )
            )
        )

        val navigation = UserGuideCategory(
            context.getString(R.string.navigation), listOf(
                UserGuide(
                    context.getString(R.string.guide_beacons_title),
                    context.getString(R.string.guide_beacons_description),
                    R.raw.beacons
                ),
                UserGuide(
                    context.getString(R.string.guide_create_beacons_title),
                    context.getString(R.string.guide_create_beacons_description),
                    R.raw.create_beacon
                ),
                UserGuide(
                    context.getString(R.string.tool_backtrack_title),
                    context.getString(R.string.guide_backtrack_description),
                    R.raw.using_backtrack
                ),
                UserGuide(
                    context.getString(R.string.guide_location_no_gps_title),
                    context.getString(R.string.guide_location_no_gps_description),
                    R.raw.determine_location_without_gps
                )
            )
        )

        val weather = UserGuideCategory(
            context.getString(R.string.weather), listOf(
                UserGuide(
                    context.getString(R.string.guide_weather_prediction_title),
                    context.getString(R.string.guide_weather_prediction_description),
                    R.raw.weather
                ),
                UserGuide(
                    context.getString(R.string.guide_barometer_calibration_title),
                    context.getString(R.string.guide_barometer_calibration_description),
                    R.raw.calibrating_barometer
                ),
                UserGuide(
                    context.getString(R.string.guide_thermometer_calibration_title),
                    context.getString(R.string.guide_thermometer_calibration_description),
                    R.raw.calibrating_thermometer
                )
            )
        )

        val tools = UserGuideCategory(
            context.getString(R.string.tools), listOf(
                UserGuide(
                    context.getString(R.string.guide_avalanche_risk),
                    context.getString(R.string.guide_avalanche_description),
                    R.raw.determine_avalanche_risk
                ),
                UserGuide(
                    context.getString(R.string.object_height_guide),
                    context.getString(R.string.object_height_guide_description),
                    R.raw.height_estimation
                ),
                UserGuide(
                    context.getString(R.string.cliff_height_guide),
                    context.getString(R.string.cliff_height_guide_description),
                    R.raw.cliff_height
                ),
                UserGuide(
                    context.getString(R.string.guide_light_meter_title),
                    context.getString(R.string.guide_light_meter_description),
                    R.raw.flashlight_testing
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