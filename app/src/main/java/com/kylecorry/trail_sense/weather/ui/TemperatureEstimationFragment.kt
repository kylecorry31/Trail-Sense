package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTemperatureEstimationBinding
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trailsensecore.domain.math.toFloatCompat
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.Temperature
import com.kylecorry.trailsensecore.domain.units.TemperatureUnits
import com.kylecorry.trailsensecore.domain.weather.WeatherService
import com.kylecorry.trailsensecore.infrastructure.sensors.read
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer
import com.kylecorry.trailsensecore.infrastructure.view.BoundFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class TemperatureEstimationFragment : BoundFragment<FragmentTemperatureEstimationBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val altimeter by lazy { sensorService.getAltimeter(false) }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val temperatureUnits by lazy { prefs.temperatureUnits }
    private val distanceUnits by lazy { prefs.baseDistanceUnits }
    private val weatherService = WeatherService()
    private val formatService by lazy { FormatServiceV2(requireContext()) }

    private val intervalometer = Intervalometer {
        if (!isBound) {
            return@Intervalometer
        }
        val temp = getEstimation()
        binding.destTemperature.text = if (temp == null) {
            getString(R.string.dash)
        } else {
            formatService.formatTemperature(temp.convertTo(temperatureUnits))
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTemperatureEstimationBinding {
        return FragmentTemperatureEstimationBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.baseElevation.units = listOf(
            DistanceUnits.Feet,
            DistanceUnits.Yards,
            DistanceUnits.Miles,
            DistanceUnits.NauticalMiles,
            DistanceUnits.Meters,
            DistanceUnits.Kilometers
        )
        binding.destElevation.units = listOf(
            DistanceUnits.Feet,
            DistanceUnits.Yards,
            DistanceUnits.Miles,
            DistanceUnits.NauticalMiles,
            DistanceUnits.Meters,
            DistanceUnits.Kilometers
        )
        // TODO: Use current temperature button
        // TODO: Use current altitude button
        lifecycleScope.launch {
            setup()
        }
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(200)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    private suspend fun setup() {
        withContext(Dispatchers.Main) {
            binding.loading.isVisible = true
            binding.temperatureEstimationInput.isVisible = false
        }

        withContext(Dispatchers.IO) {
            altimeter.read()
            thermometer.read()
        }

        val temp = Temperature(
            getCalibratedReading(thermometer.temperature),
            TemperatureUnits.C
        ).convertTo(
            prefs.temperatureUnits
        )
        binding.baseElevation.updateDistance(
            Distance.meters(altimeter.altitude).convertTo(distanceUnits)
        )
        binding.baseTemperature.setText(temp.temperature.roundToInt().toString())

        withContext(Dispatchers.Main) {
            binding.loading.isVisible = false
            binding.temperatureEstimationInput.isVisible = true
        }
    }


    private fun getEstimation(): Temperature? {
        val baseTemp = getBaseTemperature()
        val baseElevation = getBaseElevation()
        val destElevation = getDestElevation()

        if (baseTemp == null || baseElevation == null || destElevation == null) {
            return null
        }

        return weatherService.getTemperatureAtElevation(baseTemp, baseElevation, destElevation)
    }

    private fun getBaseTemperature(): Temperature? {
        val ui = binding.baseTemperature.text.toString().toFloatCompat() ?: return null
        val uiTemp = Temperature(ui, temperatureUnits)
        return uiTemp.convertTo(TemperatureUnits.C)
    }

    private fun getBaseElevation(): Distance? {
        val elevation = binding.baseElevation.distance ?: return null
        return elevation.meters()
    }

    private fun getDestElevation(): Distance? {
        val elevation = binding.destElevation.distance ?: return null
        return elevation.meters()
    }

    // TODO: Extract this
    private fun getCalibratedReading(temp: Float): Float {
        val calibrated1 = prefs.weather.minActualTemperature
        val uncalibrated1 = prefs.weather.minBatteryTemperature
        val calibrated2 = prefs.weather.maxActualTemperature
        val uncalibrated2 = prefs.weather.maxBatteryTemperature

        return calibrated1 + (calibrated2 - calibrated1) * (uncalibrated1 - temp) / (uncalibrated1 - uncalibrated2)
    }

}