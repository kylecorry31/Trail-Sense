package com.kylecorry.trail_sense.weather.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.core.sensors.read
import com.kylecorry.andromeda.core.time.Timer
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.andromeda.core.units.Temperature
import com.kylecorry.andromeda.core.units.TemperatureUnits
import com.kylecorry.andromeda.forms.*
import com.kylecorry.andromeda.forms.Forms.add
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentTemperatureEstimationBinding
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.*
import com.kylecorry.trailsensecore.domain.weather.WeatherService
import kotlinx.coroutines.*
import java.time.Duration
import kotlin.math.roundToInt

class TemperatureEstimationFragment : BoundFragment<FragmentTemperatureEstimationBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val altimeter by lazy { sensorService.getAltimeter(false) }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val temperatureUnits by lazy { prefs.temperatureUnits }
    private val baseUnits by lazy { prefs.baseDistanceUnits }
    private val weatherService = WeatherService()
    private val formatService by lazy { FormatService(requireContext()) }

    private var form: Forms.Section? = null

    private val intervalometer = Timer {
        if (!isBound) {
            return@Timer
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

        val distanceUnits = formatService.sortDistanceUnits(
            listOf(
                DistanceUnits.Feet,
                DistanceUnits.Yards,
                DistanceUnits.Miles,
                DistanceUnits.NauticalMiles,
                DistanceUnits.Meters,
                DistanceUnits.Kilometers
            )
        )

        val tempHint = if (temperatureUnits == TemperatureUnits.C) {
            getString(R.string.celsius)
        } else {
            getString(R.string.fahrenheit)
        }


        form = Forms.Section(requireContext()) {
            distance(
                "base",
                distanceUnits,
                defaultUnit = baseUnits,
                label = getString(R.string.base_elevation),
                hint = getString(R.string.altitude)
            )
            distance(
                "destination",
                distanceUnits,
                defaultUnit = baseUnits,
                label = getString(R.string.destination_elevation),
                hint = getString(R.string.altitude)
            )
            number("temperature", label = getString(R.string.base_temperature), hint = tempHint)
            button("autofill", label = getString(R.string.autofill)) {
                autofill()
            }
            loading("loading")
        }

        form?.hide("loading")

        binding.temperatureEstimationInput.add(form!!)

        setFieldsFromSensors()
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(200)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    private fun autofill() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                form?.hide("autofill")
                form?.show("loading")
                form?.get<DistanceField>("base")?.isEnabled = false
                form?.get<NumberTextField>("temperature")?.isEnabled = false
            }
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(Duration.ofSeconds(10).toMillis()) {
                    val jobs = mutableListOf<Job>()
                    jobs.add(launch { altimeter.read() })
                    jobs.add(launch { thermometer.read() })
                    jobs.joinAll()
                }
            }

            withContext(Dispatchers.Main) {
                setFieldsFromSensors()
                form?.show("autofill")
                form?.hide("loading")
                form?.get<DistanceField>("base")?.isEnabled = true
                form?.get<NumberTextField>("temperature")?.isEnabled = true
            }
        }
    }

    private fun setFieldsFromSensors() {
        if (altimeter.hasValidReading) {
            val altitude = Distance.meters(altimeter.altitude).convertTo(baseUnits)
            form?.setValue<Distance?>("base", altitude)
        }

        if (thermometer.hasValidReading) {
            tryOrNothing {
                val temp = Temperature(
                    getCalibratedReading(thermometer.temperature),
                    TemperatureUnits.C
                ).convertTo(
                    prefs.temperatureUnits
                )
                form?.setValue<Number>("temperature", temp.temperature.roundToInt())
            }
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
        val ui = form?.getValue<Number?>("temperature")?.toFloat() ?: return null
        val uiTemp = Temperature(ui, temperatureUnits)
        return uiTemp.convertTo(TemperatureUnits.C)
    }

    private fun getBaseElevation(): Distance? {
        val value = form?.getValue<Distance?>("base") ?: return null
        return value.meters()
    }

    private fun getDestElevation(): Distance? {
        val value = form?.getValue<Distance?>("destination") ?: return null
        return value.meters()
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