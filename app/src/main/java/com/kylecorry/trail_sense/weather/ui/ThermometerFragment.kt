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
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trail_sense.weather.domain.WeatherService
import com.kylecorry.trailsensecore.domain.units.TemperatureUnits
import com.kylecorry.trailsensecore.domain.weather.HeatAlert
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

    private val readings = mutableListOf<Float>()
    private var lastReadingTime = Instant.MIN
    private lateinit var chart: TemperatureChart

    private val chartReadings = mutableListOf<Float>()
    private val maxChartReadings = 300f
    private lateinit var units: TemperatureUnits

    private var useDifferential = false

    private var heatAlertTitle = ""
    private var heatAlertContent = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentThermometerHygrometerBinding.inflate(inflater, container, false)
        chart = TemperatureChart(
            binding.tempChart,
            UiUtils.color(requireContext(), R.color.colorPrimary)
        )

        binding.heatAlert.setOnClickListener {
            UiUtils.alert(requireContext(), heatAlertTitle, heatAlertContent, R.string.dialog_ok)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        units = prefs.temperatureUnits
        thermometer.start(this::onTemperatureUpdate)
        hygrometer.start(this::onHumidityUpdate)
    }

    override fun onPause() {
        super.onPause()
        thermometer.stop(this::onTemperatureUpdate)
        hygrometer.stop(this::onHumidityUpdate)
    }

    private fun updateUI() {

        val hasTemp = thermometer.hasValidReading
        val hasHumidity = hygrometer.hasValidReading
        val uncalibrated = thermometer.temperature

        val calibrated = getCalibratedReading(thermometer.temperature)

        if (uncalibrated != 0f) {
            val formattedUnit = if (units == TemperatureUnits.C) {
                calibrated
            } else {
                calibrated * 9 / 5f + 32
            }
            chartReadings.add(formattedUnit)
            if (chartReadings.size > maxChartReadings) {
                chartReadings.removeAt(0)
            }
            chart.plot(chartReadings, units)
        }

        val reading = if (useDifferential && readings.size == 3) {
            val first = readings[0]
            val second = readings[1]
            val third = readings[2]
            newWeatherService.getAmbientTemperature(first, second, third) ?: calibrated
        } else {
            calibrated
        }

        if (!hasTemp) {
            binding.temperature.text = getString(R.string.dash)
        } else {
            binding.batteryTemp.text = getString(
                R.string.battery_temp,
                formatService.formatTemperature(uncalibrated, prefs.temperatureUnits)
            )
            binding.temperature.text =
                formatService.formatTemperature(reading, prefs.temperatureUnits)
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
                formatService.formatTemperature(dewPoint, prefs.temperatureUnits)
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
        if (Duration.between(lastReadingTime, Instant.now()) > Duration.ofMillis(8000L)) {
            readings.add(calibrated)
            if (readings.size > 3) {
                readings.removeAt(0)
            }
            lastReadingTime = Instant.now()
        }
        updateUI()
        return true
    }

    private fun onHumidityUpdate(): Boolean {
        updateUI()
        return true
    }

    private fun getCalibratedReading(temp: Float): Float {
        // TODO: Load calibration data from prefs
        val calibrated1 = -17f
        val uncalibrated1 = 4f
        val calibrated2 = 22.5f
        val uncalibrated2 = 30f

        return calibrated1 + (calibrated2 - calibrated1) * (uncalibrated1 - temp) / (uncalibrated1 - uncalibrated2)
    }
}
