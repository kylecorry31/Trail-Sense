package com.kylecorry.trail_sense.diagnostics

import android.graphics.Color
import android.hardware.Sensor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.sensors.asLiveData
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.core.units.DistanceUnits
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.barometer.Barometer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentSensorDetailsBinding
import com.kylecorry.trail_sense.databinding.ListItemSensorBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.NullBarometer
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.hygrometer.NullHygrometer
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.andromeda.core.units.Pressure
import com.kylecorry.andromeda.core.units.PressureUnits
import com.kylecorry.andromeda.core.units.Temperature
import com.kylecorry.andromeda.core.units.TemperatureUnits
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.*

class SensorDetailsFragment : BoundFragment<FragmentSensorDetailsBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private lateinit var sensorListView: ListView<SensorDetails>
    private val prefs by lazy { UserPreferences(requireContext()) }

    private val formatService by lazy { FormatService(requireContext()) }
    private val sensorDetailsMap = mutableMapOf<String, SensorDetails?>()

    private val cachedGPS by lazy { CachedGPS(requireContext(), 500) }
    private val gps by lazy { sensorService.getGPS() }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val compass by lazy { sensorService.getCompass() }
    private val cellSignal by lazy { sensorService.getCellSignal() }
    private val barometer by lazy { sensorService.getBarometer() }
    private val thermometer by lazy { sensorService.getThermometer() }
    private val hygrometer by lazy { sensorService.getHygrometer() }
    private val gravity by lazy { sensorService.getGravity() }
    private val magnetometer by lazy { sensorService.getMagnetometer() }
    private val gyroscope by lazy {
        sensorService.getGyroscope()
    }
    private val battery by lazy { Battery(requireContext()) }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSensorDetailsBinding {
        return FragmentSensorDetailsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sensorListView =
            ListView(binding.sensorsList, R.layout.list_item_sensor) { sensorView, details ->
                val itemBinding = ListItemSensorBinding.bind(sensorView)
                itemBinding.sensorName.text = details.name
                itemBinding.sensorDetails.text = details.description
                itemBinding.sensorStatus.setImageResource(details.statusIcon)
                itemBinding.sensorStatus.setStatusText(details.statusMessage)
                itemBinding.sensorStatus.setBackgroundTint(details.statusColor)
                itemBinding.sensorStatus.setForegroundTint(Color.BLACK)
            }

        sensorListView.addLineSeparator()

        cachedGPS.asLiveData().observe(viewLifecycleOwner, { updateGPSCache() })
        gps.asLiveData().observe(viewLifecycleOwner, { updateGPS() })
        compass.asLiveData().observe(viewLifecycleOwner, { updateCompass() })
        barometer.asLiveData().observe(viewLifecycleOwner, { updateBarometer() })
        altimeter.asLiveData().observe(viewLifecycleOwner, { updateAltimeter() })
        thermometer.asLiveData().observe(viewLifecycleOwner, { updateThermometer() })
        hygrometer.asLiveData().observe(viewLifecycleOwner, { updateHygrometer() })
        gravity.asLiveData().observe(viewLifecycleOwner, { updateGravity() })
        cellSignal.asLiveData().observe(viewLifecycleOwner, { updateCellSignal() })
        magnetometer.asLiveData().observe(viewLifecycleOwner, { updateMagnetometer() })
        battery.asLiveData().observe(viewLifecycleOwner, { updateBattery() })
        gyroscope.asLiveData().observe(viewLifecycleOwner, { updateGyro() })

        if (!Sensors.hasCompass(requireContext())) {
            sensorDetailsMap["compass"] = SensorDetails(
                getString(R.string.pref_compass_sensor_title),
                "",
                getString(R.string.gps_unavailable),
                CustomUiUtils.getQualityColor(Quality.Poor),
                R.drawable.ic_compass_icon
            )
        }
        scheduleUpdates(Duration.ofMillis(500))
    }

    override fun onUpdate() {
        super.onUpdate()
        updateClock()
        synchronized(this) {
            val details =
                sensorDetailsMap.toList().mapNotNull { it.second }.sortedBy { it.name }
            sensorListView.setData(details)
        }
    }

    private fun updateGyro() {
        if (!Sensors.hasSensor(requireContext(), Sensor.TYPE_GYROSCOPE)) {
            return
        }
        val euler = gyroscope.orientation.toEuler()
        sensorDetailsMap["gyroscope"] = SensorDetails(
            getString(R.string.sensor_gyroscope),
            getString(
                R.string.roll_pitch_yaw,
                formatService.formatDegrees(euler.roll),
                formatService.formatDegrees(euler.pitch),
                formatService.formatDegrees(euler.yaw)
            ),
            formatService.formatQuality(gyroscope.quality),
            CustomUiUtils.getQualityColor(gyroscope.quality),
            R.drawable.ic_gyro
        )
    }

    private fun updateClock() {
        sensorDetailsMap["clock"] = SensorDetails(
            getString(R.string.tool_clock_title),
            formatService.formatTime(LocalTime.now()) + " " + ZonedDateTime.now().zone.getDisplayName(
                TextStyle.FULL,
                Locale.getDefault()
            ),
            formatService.formatQuality(Quality.Good),
            CustomUiUtils.getQualityColor(Quality.Good),
            R.drawable.ic_tool_clock
        )
    }

    private fun updateGPSCache() {
        sensorDetailsMap["gps_cache"] = SensorDetails(
            getString(R.string.gps_cache),
            formatService.formatLocation(cachedGPS.location),
            getGPSCacheStatus(),
            CustomUiUtils.getQualityColor(getGPSCacheQuality()),
            R.drawable.satellite
        )
    }

    private fun updateAltimeter() {
        val altitude = Distance(
            altimeter.altitude,
            DistanceUnits.Meters
        ).convertTo(if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) DistanceUnits.Meters else DistanceUnits.Feet)
        sensorDetailsMap["altimeter"] = SensorDetails(
            getString(R.string.pref_altimeter_calibration_title),
            formatService.formatDistance(altitude),
            getAltimeterStatus(),
            getAltimeterColor(),
            R.drawable.ic_altitude
        )
    }

    private fun updateBattery() {
        val quality = when (battery.health) {
            BatteryHealth.Good -> Quality.Good
            BatteryHealth.Unknown -> Quality.Unknown
            else -> Quality.Poor
        }
        sensorDetailsMap["battery"] = SensorDetails(
            getString(R.string.tool_battery_title),
            formatService.formatPercentage(battery.percent),
            formatService.formatBatteryHealth(battery.health),
            CustomUiUtils.getQualityColor(quality),
            R.drawable.ic_tool_battery
        )
    }

    private fun updateBarometer() {
        if (barometer is NullBarometer) {
            return
        }
        val pressure =
            Pressure(barometer.pressure, PressureUnits.Hpa).convertTo(prefs.pressureUnits)
        sensorDetailsMap["barometer"] = SensorDetails(
            getString(R.string.barometer),
            formatService.formatPressure(pressure),
            formatService.formatQuality(barometer.quality),
            CustomUiUtils.getQualityColor(barometer.quality),
            R.drawable.ic_weather
        )
    }

    private fun updateGPS() {
        sensorDetailsMap["gps"] = SensorDetails(
            getString(R.string.gps),
            "${formatService.formatLocation(gps.location)}\n${gps.satellites} ${getString(R.string.satellites)}",
            getGPSStatus(),
            getGPSColor(),
            R.drawable.satellite
        )
    }

    private fun updateCompass() {
        sensorDetailsMap["compass"] = SensorDetails(
            getString(R.string.pref_compass_sensor_title),
            formatService.formatDegrees(compass.bearing.value, replace360 = true),
            formatService.formatQuality(compass.quality),
            CustomUiUtils.getQualityColor(compass.quality),
            R.drawable.ic_compass_icon
        )
    }

    private fun updateThermometer() {
        val temperature = Temperature(
            thermometer.temperature,
            TemperatureUnits.C
        ).convertTo(prefs.temperatureUnits)
        sensorDetailsMap["thermometer"] = SensorDetails(
            getString(R.string.tool_thermometer_title),
            formatService.formatTemperature(temperature),
            formatService.formatQuality(thermometer.quality),
            CustomUiUtils.getQualityColor(thermometer.quality),
            R.drawable.thermometer
        )
    }

    private fun updateHygrometer() {
        if (hygrometer is NullHygrometer) {
            return
        }

        sensorDetailsMap["hygrometer"] = SensorDetails(
            getString(R.string.hygrometer),
            formatService.formatPercentage(hygrometer.humidity),
            formatService.formatQuality(hygrometer.quality),
            CustomUiUtils.getQualityColor(hygrometer.quality),
            R.drawable.ic_category_water
        )
    }

    private fun updateGravity() {
        sensorDetailsMap["gravity"] = SensorDetails(
            getString(R.string.gravity),
            formatService.formatAcceleration(gravity.acceleration.magnitude(), 2),
            formatService.formatQuality(gravity.quality),
            CustomUiUtils.getQualityColor(gravity.quality),
            R.drawable.ic_tool_cliff_height
        )
    }

    private fun updateCellSignal() {
        val signal = cellSignal.signals.maxByOrNull { it.strength }
        sensorDetailsMap["cell"] = SensorDetails(
            getString(R.string.cell_signal),
            "${
                CellSignalUtils.getCellTypeString(
                    requireContext(),
                    signal?.network
                )
            }\n${formatService.formatPercentage(signal?.strength ?: 0f)} (${
                formatService.formatDbm(
                    signal?.dbm ?: 0
                )
            })",
            formatService.formatQuality(signal?.quality ?: Quality.Unknown),
            CustomUiUtils.getQualityColor(signal?.quality ?: Quality.Unknown),
            CellSignalUtils.getCellQualityImage(signal?.quality)
        )
    }

    private fun updateMagnetometer() {
        sensorDetailsMap["magnetometer"] = SensorDetails(
            getString(R.string.magnetometer),
            formatService.formatMagneticField(magnetometer.magneticField.magnitude()),
            formatService.formatQuality(magnetometer.quality),
            CustomUiUtils.getQualityColor(magnetometer.quality),
            R.drawable.ic_tool_metal_detector
        )
    }

    @ColorInt
    private fun getAltimeterColor(): Int {
        if (altimeter is OverrideAltimeter) {
            return Resources.color(requireContext(), R.color.green)
        }

        if (altimeter is CachedAltimeter) {
            return Resources.color(requireContext(), R.color.red)
        }

        if (altimeter is Barometer) {
            return CustomUiUtils.getQualityColor(altimeter.quality)
        }

        if (!altimeter.hasValidReading) {
            return Resources.color(requireContext(), R.color.yellow)
        }

        return CustomUiUtils.getQualityColor(altimeter.quality)
    }

    private fun getAltimeterStatus(): String {
        if (altimeter is OverrideAltimeter) {
            return getString(R.string.gps_user)
        }

        if (altimeter is CachedGPS) {
            return getString(R.string.gps_unavailable)
        }

        if (!altimeter.hasValidReading) {
            return getString(R.string.gps_searching)
        }

        return formatService.formatQuality(altimeter.quality)
    }

    @ColorInt
    private fun getGPSColor(): Int {
        if (gps is OverrideGPS) {
            return Resources.color(requireContext(), R.color.green)
        }

        if (gps is CachedGPS || !GPS.isAvailable(requireContext())) {
            return Resources.color(requireContext(), R.color.red)
        }

        if (Duration.between(gps.time, Instant.now()) > Duration.ofMinutes(2)) {
            return Resources.color(requireContext(), R.color.yellow)
        }

        if (!gps.hasValidReading || (prefs.requiresSatellites && gps.satellites < 4) || (gps is CustomGPS && (gps as CustomGPS).isTimedOut)) {
            return Resources.color(requireContext(), R.color.yellow)
        }

        return CustomUiUtils.getQualityColor(gps.quality)
    }

    private fun getGPSCacheStatus(): String {
        return if (cachedGPS.location == Coordinate.zero) {
            getString(R.string.gps_unavailable)
        } else {
            formatService.formatQuality(Quality.Good)
        }
    }

    private fun getGPSCacheQuality(): Quality {
        return if (cachedGPS.location == Coordinate.zero) {
            Quality.Poor
        } else {
            Quality.Good
        }
    }

    private fun getGPSStatus(): String {
        if (gps is OverrideGPS) {
            return getString(R.string.gps_user)
        }

        if (gps is CachedGPS || !GPS.isAvailable(requireContext())) {
            return getString(R.string.gps_unavailable)
        }

        if (Duration.between(gps.time, Instant.now()) > Duration.ofMinutes(2)) {
            return getString(R.string.gps_stale)
        }

        if (!gps.hasValidReading || (prefs.requiresSatellites && gps.satellites < 4) || (gps is CustomGPS && (gps as CustomGPS).isTimedOut)) {
            return getString(R.string.gps_searching)
        }

        return formatService.formatQuality(gps.quality)
    }

    data class SensorDetails(
        val name: String,
        val description: String,
        val statusMessage: String,
        @ColorInt val statusColor: Int,
        @DrawableRes val statusIcon: Int
    )

}