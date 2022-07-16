package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.topics.asLiveData
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.Weather
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ActivityWeatherBinding
import com.kylecorry.trail_sense.quickactions.WeatherQuickActionBinder
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.CustomUiUtils.setCompoundDrawables
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.weather.domain.PressureReading
import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
import com.kylecorry.trail_sense.weather.infrastructure.*
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class WeatherFragment : BoundFragment<ActivityWeatherBinding>() {

    private var useSeaLevelPressure = false
    private var units = PressureUnits.Hpa

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val temperatureUnits by lazy { prefs.temperatureUnits }

    private lateinit var chart: PressureChart

    private val formatService by lazy { FormatService(requireContext()) }

    private var history: List<WeatherObservation> = listOf()

    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(requireContext()) }
    private var weather: CurrentWeather? = null

    private val logger by lazy {
        WeatherLogger(
            requireContext(),
            Duration.ofSeconds(30),
            Duration.ofSeconds(1)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        WeatherQuickActionBinder(
            this,
            binding,
            prefs.weather
        ).bind()

        chart = PressureChart(binding.chart) { timeAgo, pressure ->
            if (timeAgo == null || pressure == null) {
                binding.pressureMarker.isInvisible = true
            } else {
                val formatted = formatService.formatPressure(
                    Pressure(pressure, units),
                    Units.getDecimalPlaces(units)
                )
                binding.pressureMarker.text = getString(
                    R.string.pressure_reading_time_ago,
                    formatted,
                    formatService.formatDuration(timeAgo, false)
                )
                binding.pressureMarker.isVisible = true
            }
        }

        weatherSubsystem.weatherChanged.asLiveData().observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                onIO {
                    history = weatherSubsystem.getHistory().filter {
                        Duration.between(it.time, Instant.now()) <= prefs.weather.pressureHistory
                    }
                    weather = weatherSubsystem.getWeather()
                }
                onMain {
                    update()
                    updateForecast()
                }
            }
        }

        binding.weatherHumidity.setOnClickListener {
            showHumidityChart()
        }

        binding.weatherTemperature.setOnClickListener {
            showTemperatureChart()
        }

        if (!Sensors.hasHygrometer(requireContext())) {
            binding.weatherHumidity.isVisible = false
        }

        binding.weatherTemperature.isVisible = prefs.weather.showTemperature
    }

    override fun onResume() {
        super.onResume()
        logger.start()
        useSeaLevelPressure = prefs.weather.useSeaLevelPressure
        units = prefs.pressureUnits

        update()

        if (!prefs.weather.shouldMonitorWeather) {
            val error = UserError(
                ErrorBannerReason.WeatherMonitorOff,
                getString(R.string.weather_monitoring_disabled),
                R.drawable.ic_weather,
                action = getString(R.string.enable)
            ) {
                prefs.weather.shouldMonitorWeather = true
                WeatherUpdateScheduler.start(requireContext())
                RequestRemoveBatteryRestrictionCommand(requireContext()).execute()
                requireMainActivity().errorBanner.dismiss(ErrorBannerReason.WeatherMonitorOff)
            }
            requireMainActivity().errorBanner.report(error)
        }
    }

    override fun onPause() {
        super.onPause()
        logger.stop()
        requireMainActivity().errorBanner.dismiss(ErrorBannerReason.WeatherMonitorOff)
    }


    private fun update() {
        if (!isBound) return
        val weather = weather ?: return
        val observation = weather.observation ?: return

        displayPressureChart(history)
        displayTendency(weather.pressureTendency)
        displayPressure(observation.pressure)
        displayTemperature(observation.temperature)
        observation.humidity?.let { displayHumidity(it) }
    }

    private fun displayPressureChart(readings: List<WeatherObservation>) {
        val displayReadings = readings.map { it.pressureReading() }

        if (displayReadings.isNotEmpty()) {
            val totalTime = Duration.between(
                displayReadings.first().time, Instant.now()
            )
            var hours = totalTime.toHours()
            val minutes = totalTime.toMinutes() % 60

            when (hours) {
                0L -> binding.pressureHistoryDuration.text = context?.resources?.getQuantityString(
                    R.plurals.last_minutes,
                    minutes.toInt(),
                    minutes
                )
                else -> {
                    if (minutes >= 30) hours++
                    binding.pressureHistoryDuration.text =
                        context?.resources?.getQuantityString(
                            R.plurals.last_hours,
                            hours.toInt(),
                            hours
                        )
                }
            }

        }

        if (displayReadings.isNotEmpty()) {
            chart.setUnits(units)

            val chartData = displayReadings.map {
                val timeAgo = Duration.between(Instant.now(), it.time).seconds / (60f * 60f)
                Pair(
                    timeAgo as Number,
                    (PressureUnitUtils.convert(
                        it.value,
                        units
                    )) as Number
                )
            }

            chart.plot(chartData)
        }
    }

    private fun displayTendency(tendency: PressureTendency) {
        val formatted = formatService.formatPressure(
            Pressure(tendency.amount, PressureUnits.Hpa).convertTo(units),
            Units.getDecimalPlaces(units) + 1
        )
        binding.weatherPressureTendency.title =
            getString(R.string.pressure_tendency_format_2, formatted)

        val imageMapper = PressureCharacteristicImageMapper()
        binding.weatherPressureTendency.setImageResource(imageMapper.getImageResource(tendency.characteristic))
    }

    private suspend fun updateForecast() {
        if (!isBound) return
        val weather = weather ?: return
        val observation = weather.observation ?: return
        val prediction = weather.prediction
        onMain {
            binding.weatherTitle.title.text = formatWeather(prediction.hourly)
            binding.weatherTitle.title.setCompoundDrawables(
                size = Resources.dp(requireContext(), 24f).toInt(),
                left = getWeatherImage(prediction.hourly, observation.pressureReading())
            )
            val speed = formatSpeed(prediction.hourly)
            binding.weatherTitle.subtitle.text = speed
            binding.weatherTitle.subtitle.isVisible = speed.isNotEmpty()
            binding.dailyForecast.text = getLongTermWeatherDescription(prediction.daily)
        }
    }

    private fun displayPressure(pressure: Pressure) {
        val formatted = formatService.formatPressure(
            pressure.convertTo(units),
            Units.getDecimalPlaces(units)
        )
        binding.weatherPressure.title = formatted
    }

    private fun displayTemperature(temperature: Temperature) {
        binding.weatherTemperature.title = formatService.formatTemperature(temperature.convertTo(temperatureUnits))
    }

    private fun displayHumidity(humidity: Float) {
        binding.weatherHumidity.title = formatService.formatPercentage(humidity)
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

    private fun getLongTermWeatherDescription(weather: Weather): String {
        return when (weather) {
            Weather.ImprovingFast, Weather.ImprovingSlow -> getString(R.string.forecast_improving)
            Weather.WorseningSlow, Weather.WorseningFast, Weather.Storm -> getString(R.string.forecast_worsening)
            else -> ""
        }
    }

    private fun formatWeather(weather: Weather): String {
        return when (weather) {
            Weather.ImprovingFast, Weather.ImprovingSlow -> getString(R.string.weather_improving)
            Weather.WorseningFast, Weather.WorseningSlow -> getString(R.string.weather_worsening)
            Weather.NoChange -> getString(R.string.weather_unchanging)
            Weather.Storm -> getString(R.string.weather_storm)
            Weather.Unknown -> "-"
        }
    }

    private fun formatSpeed(weather: Weather): String {
        return when (weather) {
            Weather.ImprovingFast, Weather.WorseningFast, Weather.Storm -> getString(R.string.very_soon)
            Weather.ImprovingSlow, Weather.WorseningSlow -> getString(R.string.soon)
            else -> ""
        }
    }

    private fun showHumidityChart() {
        val readings =
            history.filter { it.humidity != null }.map { Reading(it.humidity!!, it.time) }
        if (readings.size < 2) {
            return
        }
        val readingDuration =
            Duration.between(readings.first().time, readings.last().time)
        CustomUiUtils.showLineChart(
            this, getString(
                R.string.humidity_history,
                formatService.formatDuration(readingDuration, true)
            )
        ) {
            val chart = HumidityChart(it)
            chart.plot(readings)
        }
    }

    private fun showTemperatureChart() {
        val readings = history.map {
            Reading(it.temperature.convertTo(temperatureUnits).temperature, it.time)
        }
        if (readings.size < 2) {
            return
        }
        val readingDuration =
            Duration.between(readings.first().time, readings.last().time)
        CustomUiUtils.showLineChart(
            this, getString(
                R.string.temperature_history,
                formatService.formatDuration(readingDuration, true)
            )
        ) {
            val chart = TemperatureChart(it)
            chart.plot(readings)
        }
    }


    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): ActivityWeatherBinding {
        return ActivityWeatherBinding.inflate(layoutInflater, container, false)
    }

}
