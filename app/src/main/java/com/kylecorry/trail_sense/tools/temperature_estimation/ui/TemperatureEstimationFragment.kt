package com.kylecorry.trail_sense.tools.temperature_estimation.ui

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentToolTemperatureEstimationBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.UnitInputView
import java.time.Duration

private enum class WindSpeedUnit(val distance: DistanceUnits, val time: TimeUnits) {
    KilometersPerHour(DistanceUnits.Kilometers, TimeUnits.Hours),
    MilesPerHour(DistanceUnits.Miles, TimeUnits.Hours),
    MetersPerSecond(DistanceUnits.Meters, TimeUnits.Seconds)
}

class TemperatureEstimationFragment : BoundFragment<FragmentToolTemperatureEstimationBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val temperatureUnits by lazy { prefs.temperatureUnits }
    private val location by lazy { LocationSubsystem.getInstance(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    private val intervalometer = CoroutineTimer {
        if (!isBound) {
            return@CoroutineTimer
        }
        val temp = getEstimation()
        binding.temperatureTitle.title.text = if (temp == null || temp.value.isNaN()) {
            getString(R.string.dash)
        } else {
            formatService.formatTemperature(temp.convertTo(temperatureUnits))
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentToolTemperatureEstimationBinding {
        return FragmentToolTemperatureEstimationBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val temps = if (temperatureUnits == TemperatureUnits.Celsius) {
            listOf(TemperatureUnits.Celsius, TemperatureUnits.Fahrenheit)
        } else {
            listOf(TemperatureUnits.Fahrenheit, TemperatureUnits.Celsius)
        }.map {
            UnitInputView.DisplayUnit(
                it,
                formatService.getTemperatureUnitName(it, true),
                formatService.getTemperatureUnitName(it, false)
            )
        }

        binding.tempEstBaseElevation.hint = getString(R.string.base_elevation)
        binding.tempEstDestElevation.hint = getString(R.string.destination_elevation)

        binding.tempEstBaseTemperature.units = temps
        binding.tempEstBaseTemperature.unit = temperatureUnits
        binding.tempEstBaseTemperature.hint = getString(R.string.current_temperature)

        val speedUnits = if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) {
            listOf(
                WindSpeedUnit.KilometersPerHour,
                WindSpeedUnit.MetersPerSecond,
                WindSpeedUnit.MilesPerHour
            )
        } else {
            listOf(
                WindSpeedUnit.MilesPerHour,
                WindSpeedUnit.KilometersPerHour,
                WindSpeedUnit.MetersPerSecond
            )
        }.map {
            val name = formatService.getSpeedUnitName(it.distance, it.time, true)
            UnitInputView.DisplayUnit(it, name, name)
        }

        binding.tempEstWindSpeed.units = speedUnits
        binding.tempEstWindSpeed.unit = speedUnits.firstOrNull()?.unit
        binding.tempEstWindSpeed.hint = getString(R.string.wind_speed)
        binding.tempEstWindSpeed.allowNegative = false
        binding.tempEstHumidity.setHint(getString(R.string.humidity))
        binding.tempEstHumidity.setInputType(
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        binding.tempEstHumidity.setOnTextChangeListener {
            updateHumidityError()
        }

        binding.tempEstAutofill.setOnClickListener {
            autofill()
        }

        setThermometerFieldFromSensor()

        // Set the initial elevation
        val elevation = location.elevation.convertTo(prefs.baseDistanceUnits)
        val roundedElevation = Distance.from(
            elevation.value.roundPlaces(
                Units.getDecimalPlaces(elevation.units)
            ),
            elevation.units
        )
        binding.tempEstBaseElevation.elevation = roundedElevation
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(200)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
        binding.tempEstBaseElevation.pause()
        binding.tempEstDestElevation.pause()
    }

    private fun autofill() {
        inBackground {
            onMain {
                binding.tempEstAutofill.isVisible = false
                binding.tempEstLoading.isVisible = true
                binding.tempEstBaseTemperature.isEnabled = false
                binding.tempEstBaseElevation.autofill()
            }

            readAll(
                listOf(thermometer),
                Duration.ofSeconds(10),
                forceStopOnCompletion = true
            )

            onMain {
                setThermometerFieldFromSensor()
                binding.tempEstAutofill.isVisible = true
                binding.tempEstLoading.isVisible = false
                binding.tempEstBaseTemperature.isEnabled = true
            }
        }
    }

    private fun setThermometerFieldFromSensor() {
        val sensorTemperature = thermometer.temperature

        if ((thermometer.hasValidReading || sensorTemperature != 0f) && !sensorTemperature.isNaN()) {
            val temp = Temperature.celsius(sensorTemperature).convertTo(temperatureUnits)
            binding.tempEstBaseTemperature.amount = temp.value.safeRoundToInt()
            binding.tempEstBaseTemperature.unit = temperatureUnits
        }
    }

    private fun getEstimation(): Temperature? {
        var temp = getBaseTemperature() ?: return null
        val destElevation = getDestElevation()
        val baseElevation = getBaseElevation()
        val humidity = getHumidity()
        val windSpeed = getWindSpeed()

        if (destElevation != null) {
            if (baseElevation == null) {
                return null
            }
            temp = Meteorology.getTemperatureAtElevation(temp, baseElevation, destElevation)
        }

        if (humidity != null) {
            temp = Temperature.celsius(
                Meteorology.getHeatIndex(temp.celsius().value, humidity)
            ).convertTo(temp.units)
        }

        if (windSpeed != null) {
            temp = Meteorology.getWindChill(temp, windSpeed)
        }

        return temp
    }

    private fun getBaseTemperature(): Temperature? {
        val amount = binding.tempEstBaseTemperature.amount?.toFloat() ?: return null
        val units = binding.tempEstBaseTemperature.unit as TemperatureUnits
        val uiTemp = Temperature.from(amount, units)
        return uiTemp.convertTo(TemperatureUnits.Celsius)
    }

    private fun getBaseElevation(): Distance? {
        val distance = binding.tempEstBaseElevation.elevation ?: return null
        return distance.meters()
    }

    private fun getDestElevation(): Distance? {
        val distance = binding.tempEstDestElevation.elevation ?: return null
        return distance.meters()
    }

    private fun getHumidity(): Float? {
        return binding.tempEstHumidity.text?.toString()?.toFloatCompat()?.takeIf {
            it in 0f..100f
        }
    }

    private fun updateHumidityError() {
        val humidityText = binding.tempEstHumidity.text?.toString()
        val humidity = humidityText?.toFloatCompat()
        binding.tempEstHumidity.setError(
            if (humidityText.isNullOrEmpty() || humidity == null || humidity in 0f..100f) {
                null
            } else {
                getString(R.string.humidity_must_be_between_0_and_100)
            }
        )
    }

    private fun getWindSpeed(): Speed? {
        val amount = binding.tempEstWindSpeed.amount?.toFloat() ?: return null
        val unit = binding.tempEstWindSpeed.unit as? WindSpeedUnit ?: return null
        return Speed.from(amount, unit.distance, unit.time)
    }

}
