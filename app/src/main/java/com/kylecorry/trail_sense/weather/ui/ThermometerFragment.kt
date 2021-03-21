package com.kylecorry.trail_sense.weather.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentThermometerHygrometerBinding
import com.kylecorry.trail_sense.shared.*
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trail_sense.weather.infrastructure.persistence.PressureRepo
import com.kylecorry.trailsensecore.domain.math.MovingAverageFilter
import com.kylecorry.trailsensecore.domain.units.Temperature
import com.kylecorry.trailsensecore.domain.units.TemperatureUnits
import com.kylecorry.trailsensecore.domain.weather.HeatAlert
import com.kylecorry.trailsensecore.domain.weather.PressureAltitudeReading
import com.kylecorry.trailsensecore.infrastructure.sensors.asLiveData
import java.time.Duration
import java.time.Instant

class ThermometerFragment : Fragment() {

    private var _binding: FragmentThermometerHygrometerBinding? = null
    private val binding get() = _binding!!

    private val sensorService by lazy { SensorService(requireContext()) }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val hygrometer by lazy { sensorService.getHygrometer() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService(requireContext()) }
    private val formatService2 by lazy { FormatServiceV2(requireContext()) }
    private val newWeatherService = com.kylecorry.trailsensecore.domain.weather.WeatherService()
    private val weatherService by lazy {
        WeatherService(
            prefs.weather.stormAlertThreshold,
            prefs.weather.dailyForecastChangeThreshold,
            prefs.weather.hourlyForecastChangeThreshold,
            prefs.weather.seaLevelFactorInRapidChanges,
            prefs.weather.seaLevelFactorInTemp
        )
    }

    private val pressureRepo by lazy { PressureRepo.getInstance(requireContext()) }

    private lateinit var temperatureChart: TemperatureChart

    private val readings = mutableListOf<Float>()
    private var maxReadings = 30
    private var readingInterval = 500L
    private var lastReadingTime = Instant.MIN

    private lateinit var units: TemperatureUnits

    private var useLawOfCooling = false

    private var heatAlertTitle = ""
    private var heatAlertContent = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThermometerHygrometerBinding.inflate(inflater, container, false)

        temperatureChart =
            TemperatureChart(binding.chart, UiUtils.color(requireContext(), R.color.colorPrimary))

        binding.heatAlert.setOnClickListener {
            UiUtils.alert(requireContext(), heatAlertTitle, heatAlertContent, R.string.dialog_ok)
        }

        binding.freezingAlert.setOnClickListener {
            UiUtils.alert(
                requireContext(), getString(R.string.freezing_temperatures_warning), getString(
                    R.string.freezing_temperatures_description
                ), getString(R.string.dialog_ok)
            )
        }

        thermometer.asLiveData().observe(viewLifecycleOwner, { onTemperatureUpdate() })
        hygrometer.asLiveData().observe(viewLifecycleOwner, { updateUI() })
        pressureRepo.getPressures()
            .observe(
                viewLifecycleOwner,
                { updateChart(it.map { it.toPressureAltitudeReading() }.sortedBy { it.time }.filter { it.time <= Instant.now() }) })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        units = prefs.temperatureUnits
        useLawOfCooling = prefs.weather.useLawOfCooling
        maxReadings = prefs.weather.lawOfCoolingReadings
        readingInterval = prefs.weather.lawOfCoolingReadingInterval
    }

    private fun updateChart(readings: List<PressureAltitudeReading>) {
        val filter = MovingAverageFilter(8)
        if (readings.size >= 2) {
            val totalTime = Duration.between(
                readings.first().time, readings.last().time
            )
            var hours = totalTime.toHours()
            val minutes = totalTime.toMinutes() % 60

            when (hours) {
                0L -> binding.tempChartTitle.text =
                    getString(R.string.pref_temperature_units_title) + " - " + context?.resources?.getQuantityString(
                        R.plurals.last_minutes,
                        minutes.toInt(),
                        minutes
                    )
                else -> {
                    if (minutes >= 30) hours++
                    binding.tempChartTitle.text =
                        getString(R.string.pref_temperature_units_title) + " - " +
                                context?.resources?.getQuantityString(
                                    R.plurals.last_hours,
                                    hours.toInt(),
                                    hours
                                )
                }
            }

        }

        if (readings.isNotEmpty()) {
            val chartData = readings.map {
                val timeAgo = Duration.between(Instant.now(), it.time).seconds / (60f * 60f)
                Pair(
                    timeAgo as Number,
                    Temperature(
                        getCalibratedReading(
                            filter.filter(it.temperature.toDouble()).toFloat()
                        ), TemperatureUnits.C
                    ).convertTo(
                        prefs.temperatureUnits
                    ).temperature as Number
                )
            }

            temperatureChart.plot(chartData)
        }
    }

    private fun updateUI() {
        val hasTemp = thermometer.hasValidReading
        val hasHumidity = hygrometer.hasValidReading
        val uncalibrated = thermometer.temperature

        val calibrated = getCalibratedReading(thermometer.temperature)

        val reading = if (useLawOfCooling && readings.size == maxReadings) {
            val first = readings.subList(0, maxReadings / 3).average().toFloat()
            val second = readings.subList(maxReadings / 3, 2 * maxReadings / 3).average().toFloat()
            val third = readings.subList(2 * maxReadings / 3, readings.size).average().toFloat()
            newWeatherService.getAmbientTemperature(first, second, third) ?: calibrated
        } else {
            calibrated
        }

        if (!hasTemp) {
            binding.temperature.text = getString(R.string.dash)
        } else {
            binding.batteryTemp.text = getString(
                R.string.battery_temp,
                formatService2.formatTemperature(
                    Temperature(uncalibrated, TemperatureUnits.C).convertTo(prefs.temperatureUnits)
                )
            )
            binding.temperature.text =
                formatService2.formatTemperature(
                    Temperature(reading, TemperatureUnits.C).convertTo(
                        prefs.temperatureUnits
                    )
                )
            binding.freezingAlert.visibility = if (reading <= 0f) View.VISIBLE else View.INVISIBLE
        }

        if (!hasHumidity) {
            binding.humidity.text = getString(R.string.no_humidity_data)
        } else {
            binding.humidity.text = formatService.formatHumidity(hygrometer.humidity)
        }

        if (hasTemp && hasHumidity) {
            val heatIndex =
                weatherService.getHeatIndex(reading, hygrometer.humidity)
            val alert = weatherService.getHeatAlert(heatIndex)
            val dewPoint = weatherService.getDewPoint(reading, hygrometer.humidity)
            binding.dewPoint.text = getString(
                R.string.dew_point,
                formatService2.formatTemperature(
                    Temperature(
                        dewPoint,
                        TemperatureUnits.C
                    ).convertTo(prefs.temperatureUnits)
                )
            )
            showHeatAlert(alert)
        } else if (hasTemp) {
            val alert =
                weatherService.getHeatAlert(
                    weatherService.getHeatIndex(
                        reading,
                        50f
                    )
                )
            showHeatAlert(alert)
        } else {
            showHeatAlert(HeatAlert.Normal)
        }

    }

    private fun showHeatAlert(alert: HeatAlert) {
        if (alert != HeatAlert.Normal) {
            binding.heatAlert.visibility = View.VISIBLE
        } else {
            binding.heatAlert.visibility = View.INVISIBLE
        }

        val alertColor = when (alert) {
            HeatAlert.FrostbiteCaution, HeatAlert.FrostbiteWarning, HeatAlert.FrostbiteDanger -> UiUtils.color(
                requireContext(),
                R.color.colorAccent
            )
            else -> UiUtils.color(requireContext(), R.color.colorPrimary)
        }

        binding.heatAlert.imageTintList = ColorStateList.valueOf(alertColor)

        heatAlertTitle = getHeatAlertTitle(alert)
        heatAlertContent = getHeatAlertMessage(alert)
    }

    private fun getHeatAlertTitle(alert: HeatAlert): String {
        return when (alert) {
            HeatAlert.HeatDanger -> getString(R.string.heat_alert_heat_danger_title)
            HeatAlert.HeatAlert -> getString(R.string.heat_alert_heat_alert_title)
            HeatAlert.HeatCaution -> getString(R.string.heat_alert_heat_caution_title)
            HeatAlert.HeatWarning -> getString(R.string.heat_alert_heat_warning_title)
            HeatAlert.FrostbiteWarning -> getString(R.string.heat_alert_frostbite_warning_title)
            HeatAlert.FrostbiteCaution -> getString(R.string.heat_alert_frostbite_caution_title)
            HeatAlert.FrostbiteDanger -> getString(R.string.heat_alert_frostbite_danger_title)
            else -> getString(R.string.heat_alert_normal_title)
        }
    }

    private fun getHeatAlertMessage(alert: HeatAlert): String {
        return when (alert) {
            HeatAlert.HeatWarning, HeatAlert.HeatCaution, HeatAlert.HeatAlert, HeatAlert.HeatDanger -> getString(
                R.string.heat_alert_heat_message
            )
            HeatAlert.FrostbiteWarning, HeatAlert.FrostbiteCaution, HeatAlert.FrostbiteDanger -> getString(
                R.string.heat_alert_frostbite_message
            )
            else -> ""
        }
    }

    private fun onTemperatureUpdate(): Boolean {
        if (thermometer.temperature == 0f) {
            return true
        }

        val calibrated = getCalibratedReading(thermometer.temperature)
        if (Duration.between(lastReadingTime, Instant.now()) > Duration.ofMillis(readingInterval)) {
            readings.add(calibrated)
            while (readings.size > maxReadings) {
                readings.removeAt(0)
            }
            lastReadingTime = Instant.now()
        }
        updateUI()
        return true
    }

    private fun getCalibratedReading(temp: Float): Float {
        val calibrated1 = prefs.weather.minActualTemperature
        val uncalibrated1 = prefs.weather.minBatteryTemperature
        val calibrated2 = prefs.weather.maxActualTemperature
        val uncalibrated2 = prefs.weather.maxBatteryTemperature

        return calibrated1 + (calibrated2 - calibrated1) * (uncalibrated1 - temp) / (uncalibrated1 - uncalibrated2)
    }
}
