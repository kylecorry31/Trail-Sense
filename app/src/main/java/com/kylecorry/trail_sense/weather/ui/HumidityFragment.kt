package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentHumidityBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.infrastructure.persistence.WeatherRepo
import java.time.Duration
import java.time.Instant

class HumidityFragment : BoundFragment<FragmentHumidityBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val hygrometer by lazy { sensorService.getHygrometer() }

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val weatherService by lazy { WeatherService(prefs.weather) }
    private val repo by lazy { WeatherRepo.getInstance(requireContext()) }

    private lateinit var chart: HumidityChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chart = HumidityChart(binding.chart)

        thermometer.asLiveData().observe(viewLifecycleOwner, { updateUI() })
        hygrometer.asLiveData().observe(viewLifecycleOwner, { updateUI() })

        repo.getAllLive().observe(viewLifecycleOwner) {
            updateChart(
                it.filter { it.value.humidity != null }
                    .map { Reading(it.value.humidity!!, it.time) }
                    .sortedBy { it.time }
                    .filter { it.time <= Instant.now() }
            )
        }

        updateUI()

    }

    private fun updateChart(readings: List<Reading<Float>>) {
        if (readings.isNotEmpty()) {
            val totalTime = Duration.between(
                readings.first().time, Instant.now()
            )
            var hours = totalTime.toHours()
            val minutes = totalTime.toMinutes() % 60

            when (hours) {
                0L -> binding.chartTitle.text =
                    getString(R.string.humidity) + " - " + context?.resources?.getQuantityString(
                        R.plurals.last_minutes,
                        minutes.toInt(),
                        minutes
                    )
                else -> {
                    if (minutes >= 30) hours++
                    binding.chartTitle.text =
                        getString(R.string.humidity) + " - " +
                                context?.resources?.getQuantityString(
                                    R.plurals.last_hours,
                                    hours.toInt(),
                                    hours
                                )
                }
            }

        }

        chart.plot(readings)
    }

    private fun updateUI() {
        val temperature = weatherService.calibrateTemperature(thermometer.temperature)
        val humidity = hygrometer.humidity
        val dewPoint =
            Temperature.celsius(weatherService.getDewPoint(temperature, humidity))
                .convertTo(prefs.temperatureUnits)
        binding.humidity.text = formatService.formatPercentage(humidity)
        binding.dewPoint.text =
            getString(R.string.dew_point, formatService.formatTemperature(dewPoint))
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHumidityBinding {
        return FragmentHumidityBinding.inflate(layoutInflater, container, false)
    }
}
