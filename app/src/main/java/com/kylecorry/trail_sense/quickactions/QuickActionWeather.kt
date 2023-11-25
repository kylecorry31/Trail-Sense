package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.topics.generic.ITopic
import com.kylecorry.andromeda.core.topics.generic.replay
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.extensions.getOrNull
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem

class QuickActionWeather(btn: ImageButton, private val andromedaFragment: AndromedaFragment) :
    TopicQuickAction(btn, andromedaFragment, hideWhenUnavailable = false) {

    private val weather = WeatherSubsystem.getInstance(context)

    override fun onCreate() {
        super.onCreate()
        button.setImageResource(R.drawable.ic_weather)
        button.setOnClickListener {
            when (weather.weatherMonitorState.getOrNull()) {
                FeatureState.On -> weather.disableMonitor()
                FeatureState.Off -> {
                    weather.enableMonitor()
                    RequestRemoveBatteryRestrictionCommand(andromedaFragment).execute()
                }
                else -> fragment.toast(context.getString(R.string.weather_monitoring_disabled))
            }
        }

        button.setOnLongClickListener {
            fragment.findNavController().navigate(R.id.action_weather)
            true
        }
    }

    override val state: ITopic<FeatureState> = weather.weatherMonitorState.replay()

}