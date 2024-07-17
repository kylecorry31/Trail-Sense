package com.kylecorry.trail_sense.tools.weather.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.quickactions.ToolServiceQuickAction
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration

class QuickActionWeatherMonitor(
    btn: ImageButton,
    fragment: Fragment
) : ToolServiceQuickAction(
    btn,
    fragment,
    WeatherToolRegistration.SERVICE_WEATHER_MONITOR,
    WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_STATE_CHANGED,
    hideWhenUnavailable = false
) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_weather)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().navigateWithAnimation(R.id.action_weather)
        return true
    }

    override fun onClick() {
        super.onClick()
        fragment.inBackground {
            when (state) {
                FeatureState.On -> service?.disable()
                FeatureState.Off -> {
                    service?.enable()
                    RequestRemoveBatteryRestrictionCommand(fragment).execute()
                }

                else -> fragment.toast(context.getString(R.string.weather_monitoring_disabled))
            }
        }
    }
}