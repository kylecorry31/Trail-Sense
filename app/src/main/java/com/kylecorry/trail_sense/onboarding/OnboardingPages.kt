package com.kylecorry.trail_sense.onboarding

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.MissingSensorAlert
import com.kylecorry.trail_sense.shared.sensors.SensorService

object OnboardingPages {

    fun getPages(context: Context): List<OnboardingPage> {
        return listOfNotNull(
            OnboardingPage(
                context.getString(R.string.explore),
                context.getString(R.string.onboarding_explore),
                R.drawable.steps
            ),
            OnboardingPage(
                context.getString(R.string.tool_user_guide_title),
                context.getString(R.string.onboarding_user_guide),
                R.drawable.user_guide
            ),
            OnboardingPage(
                context.getString(R.string.privacy_and_location),
                context.getString(R.string.onboarding_privacy),
                R.drawable.ic_not_visible
            ),
            OnboardingPage(
                context.getString(R.string.app_disclaimer_message_title),
                context.getString(R.string.disclaimer_message_content),
                R.drawable.ic_tool_notes,
                context.getString(R.string.i_agree)
            ),
            if (SensorService(context).hasCompass()) {
                null
            } else {
                OnboardingPage(
                    MissingSensorAlert.getMissingSensorTitle(
                        context,
                        context.getString(R.string.pref_compass_sensor_title)
                    ),
                    MissingSensorAlert.getMissingSensorMessage(
                        context,
                        context.getString(R.string.pref_compass_sensor_title)
                    ),
                    R.drawable.ic_compass_icon
                )
            }
        )
    }

}