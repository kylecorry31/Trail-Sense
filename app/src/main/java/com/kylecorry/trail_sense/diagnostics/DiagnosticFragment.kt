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
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.SensorChecker
import com.kylecorry.andromeda.sense.barometer.Barometer
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.FragmentDiagnosticsBinding
import com.kylecorry.trail_sense.databinding.ListItemSensorBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatServiceV2
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
import com.kylecorry.trailsensecore.domain.units.Pressure
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.Temperature
import com.kylecorry.trailsensecore.domain.units.TemperatureUnits
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.*

class DiagnosticFragment : BoundFragment<FragmentDiagnosticsBinding>() {

    private val sensorService by lazy { SensorService(requireContext()) }
    private val sensorChecker by lazy { SensorChecker(requireContext()) }
    private lateinit var sensorListView: ListView<SensorDetails>
    private val prefs by lazy { UserPreferences(requireContext()) }

    private val formatService by lazy { FormatServiceV2(requireContext()) }
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
    ): FragmentDiagnosticsBinding {
        return FragmentDiagnosticsBinding.inflate(layoutInflater, container, false)
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

        if (!sensorChecker.hasSensor(Sensor.TYPE_MAGNETIC_FIELD) && @Suppress("DEPRECATION") !sensorChecker.hasSensor(
                Sensor.TYPE_ORIENTATION
            )
        ) {
            sensorDetailsMap["compass"] = SensorDetails(
                getString(R.string.pref_compass_sensor_title),
                "",
                getString(R.string.gps_unavailable),
                CustomUiUtils.getQualityColor(requireContext(), Quality.Poor),
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
                sensorDetailsMap.toList().mapNotNull { it.second }.sortedWith(
                    compareBy({ it.type.ordinal }, { it.name })
                )
            sensorListView.setData(details)
        }
    }

    override fun onResume() {
        super.onResume()
        updateFlashlight()
        updatePermissions()
    }

    private fun updateGyro() {
        if (!sensorChecker.hasSensor(Sensor.TYPE_GYROSCOPE)) {
            sensorDetailsMap["gyroscope"] = SensorDetails(
                getString(R.string.sensor_gyroscope),
                "",
                getString(R.string.gps_unavailable),
                CustomUiUtils.getQualityColor(requireContext(), Quality.Poor),
                R.drawable.ic_gyro
            )
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
            CustomUiUtils.getQualityColor(requireContext(), gyroscope.quality),
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
            CustomUiUtils.getQualityColor(requireContext(), Quality.Good),
            R.drawable.ic_tool_clock
        )
    }

    private fun updateFlashlight() {
        val hasFlashlight = Torch.isAvailable(requireContext())
        sensorDetailsMap["flashlight"] = SensorDetails(
            getString(R.string.flashlight_title),
            "",
            if (hasFlashlight) getString(R.string.available) else getString(R.string.gps_unavailable),
            CustomUiUtils.getQualityColor(
                requireContext(),
                if (hasFlashlight) Quality.Good else Quality.Unknown
            ),
            R.drawable.flashlight
        )
    }

    private fun updateGPSCache() {
        sensorDetailsMap["gps_cache"] = SensorDetails(
            getString(R.string.gps_cache),
            formatService.formatLocation(cachedGPS.location),
            getGPSCacheStatus(),
            CustomUiUtils.getQualityColor(requireContext(), getGPSCacheQuality()),
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
            CustomUiUtils.getQualityColor(requireContext(), quality),
            R.drawable.ic_tool_battery
        )
    }

    private fun updatePermissions() {

        val permissions = mapOf(
            getString(R.string.camera) to Permissions.isCameraEnabled(requireContext()),
            getString(R.string.gps_location) to Permissions.canGetFineLocation(requireContext()),
            getString(R.string.permission_background_location) to Permissions.isBackgroundLocationEnabled(requireContext()),
            getString(R.string.permission_activity_recognition) to Permissions.canRecognizeActivity(requireContext()),
            getString(R.string.flashlight_title) to Permissions.canUseFlashlight(requireContext())
        )

        permissions.forEach {
            sensorDetailsMap["permission-${it.key}"] = SensorDetails(
                it.key,
                getString(R.string.permission),
                if (it.value) getString(R.string.permission_granted) else getString(R.string.permission_not_granted),
                CustomUiUtils.getQualityColor(
                    requireContext(),
                    if (it.value) Quality.Good else Quality.Poor
                ),
                if (it.value) R.drawable.ic_check else R.drawable.ic_cancel,
                DetailType.Permission
            )
        }
    }

    private fun updateBarometer() {
        if (barometer is NullBarometer) {
            sensorDetailsMap["barometer"] = SensorDetails(
                getString(R.string.barometer),
                "",
                getString(R.string.gps_unavailable),
                CustomUiUtils.getQualityColor(requireContext(), Quality.Unknown),
                R.drawable.ic_weather
            )
            return
        }
        val pressure =
            Pressure(barometer.pressure, PressureUnits.Hpa).convertTo(prefs.pressureUnits)
        sensorDetailsMap["barometer"] = SensorDetails(
            getString(R.string.barometer),
            formatService.formatPressure(pressure),
            formatService.formatQuality(barometer.quality),
            CustomUiUtils.getQualityColor(requireContext(), barometer.quality),
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
            CustomUiUtils.getQualityColor(requireContext(), compass.quality),
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
            CustomUiUtils.getQualityColor(requireContext(), thermometer.quality),
            R.drawable.thermometer
        )
    }

    private fun updateHygrometer() {
        if (hygrometer is NullHygrometer) {
            sensorDetailsMap["hygrometer"] = SensorDetails(
                getString(R.string.hygrometer),
                "",
                getString(R.string.gps_unavailable),
                CustomUiUtils.getQualityColor(requireContext(), Quality.Unknown),
                R.drawable.ic_category_water
            )
            return
        }

        sensorDetailsMap["hygrometer"] = SensorDetails(
            getString(R.string.hygrometer),
            formatService.formatPercentage(hygrometer.humidity),
            formatService.formatQuality(hygrometer.quality),
            CustomUiUtils.getQualityColor(requireContext(), hygrometer.quality),
            R.drawable.ic_category_water
        )
    }

    private fun updateGravity() {
        sensorDetailsMap["gravity"] = SensorDetails(
            getString(R.string.gravity),
            formatService.formatAcceleration(gravity.acceleration.magnitude(), 2),
            formatService.formatQuality(gravity.quality),
            CustomUiUtils.getQualityColor(requireContext(), gravity.quality),
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
            CustomUiUtils.getQualityColor(requireContext(), signal?.quality ?: Quality.Unknown),
            CellSignalUtils.getCellQualityImage(signal?.quality)
        )
    }

    private fun updateMagnetometer() {
        sensorDetailsMap["magnetometer"] = SensorDetails(
            getString(R.string.magnetometer),
            formatService.formatMagneticField(magnetometer.magneticField.magnitude()),
            formatService.formatQuality(magnetometer.quality),
            CustomUiUtils.getQualityColor(requireContext(), magnetometer.quality),
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
            return CustomUiUtils.getQualityColor(requireContext(), altimeter.quality)
        }

        if (!altimeter.hasValidReading) {
            return Resources.color(requireContext(), R.color.yellow)
        }

        return CustomUiUtils.getQualityColor(requireContext(), altimeter.quality)
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

        return CustomUiUtils.getQualityColor(requireContext(), gps.quality)
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
        @DrawableRes val statusIcon: Int,
        val type: DetailType = DetailType.Sensor
    )

    enum class DetailType {
        Permission,
        Sensor,
        Notification
    }

}