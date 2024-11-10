package com.kylecorry.trail_sense.tools.weather.summaries

import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.summaries.SmallSimpleToolSummaryView
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem

class WeatherToolSummaryView(root: FrameLayout, fragment: Fragment) : SmallSimpleToolSummaryView(
    root,
    fragment
) {
    private val weather = WeatherSubsystem.getInstance(context)
    private val formatter = FormatService.getInstance(context)
    private val prefs = UserPreferences(context)

    override fun onCreate() {
        super.onCreate()
        binding.summaryTitle.text = context.getString(R.string.weather)
        fragment.observe(weather.weatherChanged) {
            populateWeatherDetails()
        }
    }

    override fun onResume() {
        super.onResume()
        populateWeatherDetails()
    }

    private fun populateWeatherDetails() {
        fragment.inBackground {
            val current = weather.getWeather()

            onMain {
                binding.summarySubtitle.text = if (current.observation?.temperature != null) {
                    context.getString(
                        R.string.dot_separated_pair,
                        formatter.formatWeather(current.prediction.primaryHourly),
                        formatter.formatTemperature(
                            current.observation.temperature.convertTo(prefs.temperatureUnits)
                        )
                    )
                } else {
                    formatter.formatWeather(current.prediction.primaryHourly)
                }
                binding.summaryIcon.setImageResource(formatter.getWeatherImage(current.prediction.primaryHourly))
            }
        }
    }

    override fun onClick() {
        super.onClick()
        fragment.findNavController().openTool(Tools.WEATHER)
    }
}