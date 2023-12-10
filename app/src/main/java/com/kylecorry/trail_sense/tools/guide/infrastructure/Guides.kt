package com.kylecorry.trail_sense.tools.guide.infrastructure

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory
import com.kylecorry.trail_sense.tools.ui.Tools
import com.kylecorry.trail_sense.tools.ui.sort.AlphabeticalToolSort
import com.kylecorry.trail_sense.tools.ui.sort.CategoricalToolSort

object Guides {

    fun guides(context: Context): List<UserGuideCategory> {
        val tools = Tools.getTools(context)
        val sortedTools = CategoricalToolSort(context).sort(tools)

        val otherGuides = UserGuideCategory(
            "TO BE UPDATED",
            listOfNotNull(
                UserGuide(
                    context.getString(R.string.guide_signaling_for_help_title),
                    null,
                    R.raw.signaling_for_help
                ),
                UserGuide(
                    context.getString(R.string.navigation),
                    context.getString(R.string.navigation_guide_description),
                    R.raw.navigate
                ),
                UserGuide(
                    context.getString(R.string.guide_using_printed_maps),
                    null,
                    R.raw.using_printed_maps
                ),
                UserGuide(
                    context.getString(R.string.guide_location_no_gps_title),
                    null,
                    R.raw.determine_location_without_gps
                ),
                if (Sensors.hasBarometer(context)) UserGuide(
                    context.getString(R.string.guide_barometer_calibration_title),
                    null,
                    R.raw.calibrating_barometer
                ) else null,
                UserGuide(
                    context.getString(R.string.guide_thermometer_calibration_title),
                    null,
                    R.raw.calibrating_thermometer
                ),
                if (Sensors.hasSensor(context, Sensor.TYPE_STEP_COUNTER))
                    UserGuide(
                        context.getString(R.string.pedometer),
                        null,
                        R.raw.pedometer
                    ) else null,
                UserGuide(
                    context.getString(R.string.tides),
                    null,
                    R.raw.tides
                ),
                UserGuide(
                    context.getString(R.string.guide_recommended_apps),
                    context.getString(R.string.guide_recommended_apps_description),
                    R.raw.recommended_apps
                )
            )
        )

        val toolGuides = sortedTools.mapNotNull { category ->
            val guides = category.tools.mapNotNull { tool ->
                if (tool.guideId == null) {
                    return@mapNotNull null
                }

                // TODO: Remove this once the guides are updated
                // If the guide hasn't been updated, don't show it
                if (otherGuides.guides.any { it.contents == tool.guideId }) {
                    return@mapNotNull null
                }

                UserGuide(
                    tool.name,
                    tool.description,
                    tool.guideId
                )
            }

            if (guides.isEmpty()){
                return@mapNotNull null
            }

            UserGuideCategory(
                category.categoryName ?: context.getString(R.string.tools),
                guides
            )
        }


        return toolGuides + otherGuides
    }
}