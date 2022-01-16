package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ActivityWeatherBinding
import com.kylecorry.trail_sense.quickactions.WeatherQuickActionBinder
import com.kylecorry.trail_sense.shared.CustomUiUtils.setCompoundDrawables
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.PressureReading
import com.kylecorry.trail_sense.weather.infrastructure.WeatherContextualService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherFragment : BoundFragment<ActivityWeatherBinding>() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val forecaster by lazy { WeatherContextualService.getInstance(requireContext()) }
    private val formatter by lazy { FormatService(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WeatherQuickActionBinder(
            this,
            binding,
            prefs.weather
        ).bind()
    }

    override fun onResume() {
        super.onResume()
        updateCurrentForecast()
    }

    private fun updateCurrentForecast() {
        runInBackground {
            val current = withContext(Dispatchers.IO) {
                forecaster.getHourlyForecast()
            }

            val currentPressure = withContext(Dispatchers.IO) {
                forecaster.getLastReading()
            }

            withContext(Dispatchers.Main) {
                if (isBound) {
                    binding.pressure.text = formatWeather(current)
                    binding.tendencyAmount.text = formatSpeed(current)
                    binding.pressure.setCompoundDrawables(
                        size = Resources.dp(requireContext(), 24f).toInt(),
                        left = getWeatherImage(current, currentPressure)
                    )
                }
            }

        }
    }

    private fun formatWeather(weather: Weather): String {
        return when (weather) {
            Weather.ImprovingFast, Weather.ImprovingSlow -> "Improving"
            Weather.WorseningFast, Weather.WorseningSlow -> "Worsening"
            Weather.NoChange -> "Unchanging"
            Weather.Storm -> "Storm"
            Weather.Unknown -> "-"
        }
    }

    private fun formatSpeed(weather: Weather): String {
        return when (weather) {
            Weather.ImprovingFast, Weather.WorseningFast, Weather.Storm -> "Soon"
            Weather.ImprovingSlow, Weather.WorseningSlow -> "Later"
            else -> ""
        }
    }


    private fun getWeatherImage(weather: Weather, currentPressure: PressureReading?): Int {
        return when (weather) {
            Weather.ImprovingFast -> if (currentPressure?.isLow() == true) R.drawable.cloudy else R.drawable.sunny
            Weather.ImprovingSlow -> if (currentPressure?.isHigh() == true) R.drawable.sunny else R.drawable.partially_cloudy
            Weather.WorseningSlow -> if (currentPressure?.isLow() == true) R.drawable.light_rain else R.drawable.cloudy
            Weather.WorseningFast -> if (currentPressure?.isLow() == true) R.drawable.heavy_rain else R.drawable.light_rain
            Weather.Storm -> R.drawable.storm
            else -> R.drawable.steady
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): ActivityWeatherBinding {
        return ActivityWeatherBinding.inflate(layoutInflater, container, false)
    }

}
