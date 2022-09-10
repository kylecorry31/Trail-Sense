package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
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
            prefs.leftButton,
            binding.weatherTitle.leftButton
        ).bind(fragment)

        getQuickActionButton(
            prefs.rightButton,
            binding.weatherTitle.rightButton
        ).bind(fragment)
    }

    private fun getQuickActionButton(
        type: QuickActionType,
        button: ImageButton
    ): QuickActionButton {
        return when (type) {
            QuickActionType.Whistle -> QuickActionWhistle(button, fragment)
            QuickActionType.Flashlight -> QuickActionFlashlight(button, fragment)
            QuickActionType.Clouds -> QuickActionClouds(button, fragment)
            QuickActionType.Temperature -> QuickActionThermometer(button, fragment)
            QuickActionType.LowPowerMode -> LowPowerQuickAction(button, fragment)
            QuickActionType.Thunder -> QuickActionThunder(button, fragment)
            else -> QuickActionNone(button, fragment)
        }
    }
}