package com.kylecorry.trail_sense.shared.sensors.providers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.view.Surface
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.accelerometer.GravitySensor
import com.kylecorry.andromeda.sense.accelerometer.LowPassAccelerometer
import com.kylecorry.andromeda.sense.compass.Compass
import com.kylecorry.andromeda.sense.compass.FilteredCompass
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.compass.LegacyCompass
import com.kylecorry.andromeda.sense.magnetometer.LowPassMagnetometer
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.andromeda.sense.orientation.CustomGeomagneticRotationSensor
import com.kylecorry.andromeda.sense.orientation.GeomagneticRotationSensor
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.andromeda.sense.orientation.RotationSensor
import com.kylecorry.sol.math.filters.MovingAverageFilter
import com.kylecorry.trail_sense.settings.infrastructure.ICompassPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource
import com.kylecorry.trail_sense.shared.sensors.compass.MagQualityCompassWrapper
import com.kylecorry.trail_sense.shared.sensors.compass.NullCompass
import com.kylecorry.trail_sense.shared.sensors.compass.NullOrientationSensor

class CompassProvider(private val context: Context, private val prefs: ICompassPreferences) {

    fun get(): ICompass {
        val smoothing = prefs.compassSmoothing
        val useTrueNorth = prefs.useTrueNorth

        var source = prefs.source

        // Handle if the available sources have changed (not likely)
        val allSources = getAvailableSources(context)

        // There were no compass sensors found
        if (allSources.isEmpty()) {
            return NullCompass()
        }

        if (!allSources.contains(source)) {
            source = allSources.firstOrNull() ?: CompassSource.CustomMagnetometer
        }

        val compass = when (source) {
            CompassSource.RotationVector -> {
                Compass(
                    RotationSensor(context, SensorService.MOTION_SENSOR_DELAY),
                    useTrueNorth,
                    surfaceRotation = Surface.ROTATION_90,
                    offset = -90f
                )
            }

            CompassSource.GeomagneticRotationVector -> {
                Compass(
                    GeomagneticRotationSensor(context, SensorService.MOTION_SENSOR_DELAY),
                    useTrueNorth,
                    surfaceRotation = Surface.ROTATION_90,
                    offset = -90f
                )
            }

            CompassSource.CustomMagnetometer -> {
                Compass(
                    getCustomGeomagneticRotationSensor(),
                    useTrueNorth,
                    surfaceRotation = Surface.ROTATION_90,
                    offset = -90f
                )
            }

            CompassSource.Orientation -> {
                LegacyCompass(context, useTrueNorth, SensorService.MOTION_SENSOR_DELAY)
            }
        }

        return MagQualityCompassWrapper(
            FilteredCompass(
                compass,
                MovingAverageFilter((smoothing * 4).coerceAtLeast(1))
            ),
            Magnetometer(context, SensorManager.SENSOR_DELAY_NORMAL)
        )
    }

    fun getOrientationSensor(): IOrientationSensor {
        var source = prefs.source

        // Swap out the legacy orientation sensor for the rotation vector sensor
        if (source == CompassSource.Orientation) {
            source = CompassSource.RotationVector
        }

        // Handle if the available sources have changed (not likely)
        val allSources = getAvailableSources(context)

        // There were no compass sensors found
        if (allSources.isEmpty()) {
            return NullOrientationSensor()
        }

        if (!allSources.contains(source)) {
            source = allSources.firstOrNull() ?: CompassSource.CustomMagnetometer
            if (source == CompassSource.Orientation) {
                source = CompassSource.CustomMagnetometer
            }
        }

        // TODO: Apply the smoothing / quality to the orientation sensor
        if (source == CompassSource.RotationVector) {
            return RotationSensor(context, SensorService.MOTION_SENSOR_DELAY)
        }

        if (source == CompassSource.GeomagneticRotationVector) {
            return GeomagneticRotationSensor(
                context,
                SensorService.MOTION_SENSOR_DELAY
            )
        }

        return getCustomGeomagneticRotationSensor()
    }

    private fun getCustomGeomagneticRotationSensor(): CustomGeomagneticRotationSensor {
        val magnetometer =
            LowPassMagnetometer(context, SensorService.MOTION_SENSOR_DELAY, MAGNETOMETER_LOW_PASS)
        val accelerometer = if (Sensors.hasGravity(context)) {
            GravitySensor(context, SensorService.MOTION_SENSOR_DELAY)
        } else {
            LowPassAccelerometer(context, SensorService.MOTION_SENSOR_DELAY)
        }
        return CustomGeomagneticRotationSensor(magnetometer, accelerometer)
    }

    companion object {

        private const val MAGNETOMETER_LOW_PASS = 0.3f

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