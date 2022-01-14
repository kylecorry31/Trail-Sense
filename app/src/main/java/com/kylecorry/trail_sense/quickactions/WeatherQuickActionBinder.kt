package com.kylecorry.trail_sense.quickactions

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.databinding.ActivityWeatherBinding
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.QuickActionType
import com.kylecorry.trail_sense.shared.views.QuickActionNone
import com.kylecorry.trail_sense.weather.infrastructure.WeatherPreferences
import com.kylecorry.trail_sense.weather.ui.WeatherFragment

class WeatherQuickActionBinder(
    private val fragment: WeatherFragment,
    private val binding: ActivityWeatherBinding,
    private val prefs: WeatherPreferences
) : IQuickActionBinder {

    override fun bind() {
        getQuickActionButton(
            prefs.leftQuickAction,
            binding.weatherLeftQuickAction
        ).bind(fragment)

        getQuickActionButton(
            prefs.rightQuickAction,
            binding.weatherRightQuickAction
        ).bind(fragment)
    }

    private fun getQuickActionButton(
        type: QuickActionType,
        button: FloatingActionButton
    ): QuickActionButton {
        return when (type) {
            QuickActionType.Whistle -> QuickActionWhistle(button, fragment)
            QuickActionType.Flashlight -> QuickActionFlashlight(button, fragment)
            QuickActionType.Clouds -> QuickActionClouds(button, fragment)
            QuickActionType.Temperature -> QuickActionThermometer(button, fragment)
            QuickActionType.LowPowerMode -> LowPowerQuickAction(button, fragment)
            else -> QuickActionNone(button, fragment)
        }
    }
}