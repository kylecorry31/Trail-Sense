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
import com.kylecorry.andromeda.sense.mock.MockMagnetometer
import com.kylecorry.andromeda.sense.orientation.CustomGeomagneticRotationSensor
import com.kylecorry.andromeda.sense.orientation.GeomagneticRotationSensor
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.andromeda.sense.orientation.RotationSensor
import com.kylecorry.andromeda.sense.orientation.filter.FilteredOrientationSensor
import com.kylecorry.andromeda.sense.orientation.filter.LowPassOrientationSensorFilter
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.filters.MovingAverageFilter
import com.kylecorry.trail_sense.settings.infrastructure.ICompassPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource
import com.kylecorry.trail_sense.shared.sensors.compass.MagQualityCompassWrapper
import com.kylecorry.trail_sense.shared.sensors.compass.MockCompass
import com.kylecorry.trail_sense.shared.sensors.compass.QuickRecalibrationOrientationSensor
import kotlin.math.pow

class CompassProvider(private val context: Context, private val prefs: ICompassPreferences) {

    fun get(): ICompass {
        val useTrueNorth = prefs.useTrueNorth

        var source = prefs.source

        // Handle if the available sources have changed (not likely)
        val allSources = getAvailableSources(context)

        // There were no compass sensors found
        if (allSources.isEmpty()) {
            return MockCompass()
        }

        if (!allSources.contains(source)) {
            source = allSources.firstOrNull() ?: CompassSource.CustomMagnetometer
        }

        val compass = when (source) {
            CompassSource.Orientation -> {
                FilteredCompass(
                    LegacyCompass(context, useTrueNorth, SensorService.MOTION_SENSOR_DELAY),
                    MovingAverageFilter((prefs.compassSmoothing * 4).coerceAtLeast(1))
                )
            }

            else -> {
                Compass(
                    getOrientationSensor(),
                    useTrueNorth,
                    surfaceRotation = Surface.ROTATION_90,
                    offset = -90f
                )
            }
        }

        return MagQualityCompassWrapper(
            compass,
            Magnetometer(context, SensorManager.SENSOR_DELAY_NORMAL)
        )
    }

    private fun getBaseOrientationSensor(): IOrientationSensor {
        var source = prefs.source

        // Swap out the legacy orientation sensor for the rotation vector sensor
        if (source == CompassSource.Orientation) {
            source = CompassSource.RotationVector
        }

        val allSources = getAvailableSources(context)

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

        return getCustomGeomagneticRotationSensor(true)
    }

    fun getOrientationSensor(): IOrientationSensor {
        val smoothing = prefs.compassSmoothing

        val quickRecalibration = QuickRecalibrationOrientationSensor(
            getCustomGeomagneticRotationSensor(false),
            getBaseOrientationSensor(),
            1f,
            45f
        )

        // Smoothing isn't needed
        if (smoothing <= 1) {
            return quickRecalibration
        }

        val alpha = SolMath.map((1 - smoothing / 100f).pow(2), 0f, 1f, 0.005f, 1f)

        return FilteredOrientationSensor(
            quickRecalibration,
            LowPassOrientationSensorFilter(alpha, true)
        )

    }

    private fun getCustomGeomagneticRotationSensor(useGyroIfAvailable: Boolean): CustomGeomagneticRotationSensor {
        val magnetometer = if (Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)) {
            LowPassMagnetometer(context, SensorService.MOTION_SENSOR_DELAY, MAGNETOMETER_LOW_PASS)
        } else {
            MockMagnetometer()
        }
        val accelerometer = if (useGyroIfAvailable && Sensors.hasGravity(context)) {
            GravitySensor(context, SensorService.MOTION_SENSOR_DELAY)
        } else {
            LowPassAccelerometer(context, SensorService.MOTION_SENSOR_DELAY, ACCELEROMETER_LOW_PASS)
        }

        return CustomGeomagneticRotationSensor(magnetometer, accelerometer)
    }

    companion object {

        private const val MAGNETOMETER_LOW_PASS = 0.3f
        private const val ACCELEROMETER_LOW_PASS = 0.1f

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