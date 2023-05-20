package com.kylecorry.trail_sense.shared.sensors.providers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.compass.FilterCompassWrapper
import com.kylecorry.andromeda.sense.compass.GravityCompensatedCompass
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.compass.LegacyCompass
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.andromeda.sense.orientation.GeomagneticRotationSensor
import com.kylecorry.andromeda.sense.orientation.RotationSensor
import com.kylecorry.sol.math.filters.MovingAverageFilter
import com.kylecorry.trail_sense.settings.infrastructure.ICompassPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource
import com.kylecorry.trail_sense.shared.sensors.compass.MagQualityCompassWrapper

class CompassProvider(private val context: Context, private val prefs: ICompassPreferences) {

    fun get(): ICompass {
        val smoothing = prefs.compassSmoothing
        val useTrueNorth = prefs.useTrueNorth

        var source = prefs.source

        // Handle if the available sources have changed (not likely)
        val allSources = getAvailableSources(context)
        if (!allSources.contains(source)) {
            source = allSources.firstOrNull() ?: CompassSource.CustomMagnetometer
        }

        val compass = when (source) {
            CompassSource.RotationVector -> {
                RotationSensor(context, useTrueNorth, SensorService.MOTION_SENSOR_DELAY)
            }

            CompassSource.GeomagneticRotationVector -> {
                GeomagneticRotationSensor(context, useTrueNorth, SensorService.MOTION_SENSOR_DELAY)
            }

            CompassSource.CustomMagnetometer -> {
                GravityCompensatedCompass(context, useTrueNorth, SensorService.MOTION_SENSOR_DELAY)
            }

            CompassSource.Orientation -> {
                LegacyCompass(context, useTrueNorth, SensorService.MOTION_SENSOR_DELAY)
            }
        }

        return MagQualityCompassWrapper(
            FilterCompassWrapper(
                compass,
                MovingAverageFilter((smoothing * 4).coerceAtLeast(1))
            ),
            Magnetometer(context, SensorManager.SENSOR_DELAY_NORMAL)
        )
    }

    companion object {
        /**
         * Returns the available compass sources in order of quality
         */
        fun getAvailableSources(context: Context): List<CompassSource> {
            val sources = mutableListOf<CompassSource>()

            if (Sensors.hasSensor(context, Sensor.TYPE_ROTATION_VECTOR)) {
                sources.add(CompassSource.RotationVector)
            }

            if (Sensors.hasSensor(context, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)) {
                sources.add(CompassSource.GeomagneticRotationVector)
            }

            if (Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)) {
                sources.add(CompassSource.CustomMagnetometer)
            }

            @Suppress("DEPRECATION")
            if (Sensors.hasSensor(context, Sensor.TYPE_ORIENTATION)) {
                sources.add(CompassSource.Orientation)
            }

            return sources
        }
    }

}