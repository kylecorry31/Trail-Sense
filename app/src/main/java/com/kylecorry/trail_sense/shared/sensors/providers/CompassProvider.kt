package com.kylecorry.trail_sense.shared.sensors.providers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Range
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
import com.kylecorry.andromeda.sense.orientation.CustomRotationSensor
import com.kylecorry.andromeda.sense.orientation.GeomagneticRotationSensor
import com.kylecorry.andromeda.sense.orientation.GravityRotationSensor
import com.kylecorry.andromeda.sense.orientation.Gyroscope
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.andromeda.sense.orientation.RotationSensor
import com.kylecorry.andromeda.sense.orientation.filter.FilteredOrientationSensor
import com.kylecorry.andromeda.sense.orientation.filter.LowPassOrientationSensorFilter
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.filters.MovingAverageFilter
import com.kylecorry.trail_sense.settings.infrastructure.ICompassPreferences
import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource
import com.kylecorry.trail_sense.shared.sensors.compass.MagQualityCompassWrapper
import com.kylecorry.trail_sense.shared.sensors.compass.MagQualityOrientationWrapper
import com.kylecorry.trail_sense.shared.sensors.compass.MockCompass
import com.kylecorry.trail_sense.shared.sensors.compass.QuickRecalibrationOrientationSensor
import kotlin.math.pow

class CompassProvider(private val context: Context, private val prefs: ICompassPreferences) {

    fun get(sensorDelay: Int): ICompass {
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
                    LegacyCompass(context, useTrueNorth, sensorDelay),
                    MovingAverageFilter((prefs.compassSmoothing * 4).coerceAtLeast(1))
                )
            }

            else -> {
                Compass(
                    getOrientationSensor(sensorDelay),
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

    private fun getBaseOrientationSensor(sensorDelay: Int): IOrientationSensor {
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

        if (source == CompassSource.RotationVector) {
            return RotationSensor(context, sensorDelay)
        }

        if (source == CompassSource.GeomagneticRotationVector) {
            return GeomagneticRotationSensor(
                context,
                sensorDelay
            )
        }

        if (source == CompassSource.CustomRotationVector) {
            return getCustomRotationSensor(sensorDelay)
        }

        return getCustomGeomagneticRotationSensor(true, sensorDelay)
    }

    fun getOrientationSensor(sensorDelay: Int): IOrientationSensor {
        val smoothing = prefs.compassSmoothing

        val baseOrientationSensor = getBaseOrientationSensor(sensorDelay)

        // Don't use quick recalibration for custom sensors
        val isCustomSensor = baseOrientationSensor is CustomGeomagneticRotationSensor ||
                baseOrientationSensor is CustomRotationSensor ||
                baseOrientationSensor is GravityRotationSensor

        val quickRecalibration =
            if (isCustomSensor) {
                baseOrientationSensor
            } else {
                QuickRecalibrationOrientationSensor(
                    getCustomGeomagneticRotationSensor(false, sensorDelay),
                    baseOrientationSensor,
                    1f,
                    45f
                )
            }

        // Smoothing isn't needed
        if (smoothing <= 1) {
            return MagQualityOrientationWrapper(
                quickRecalibration,
                Magnetometer(context, SensorManager.SENSOR_DELAY_NORMAL)
            )
        }

        val alpha = SolMath.map((1 - smoothing / 100f).pow(2), 0f, 1f, 0.005f, 1f)

        return MagQualityOrientationWrapper(
            FilteredOrientationSensor(
                quickRecalibration,
                LowPassOrientationSensorFilter(alpha, true)
            ),
            Magnetometer(context, SensorManager.SENSOR_DELAY_NORMAL)
        )

    }

    private fun getCustomGeomagneticRotationSensor(
        useGyroIfAvailable: Boolean,
        sensorDelay: Int
    ): IOrientationSensor {

        val accelerometer = if (useGyroIfAvailable && Sensors.hasGravity(context)) {
            GravitySensor(context, sensorDelay)
        } else {
            LowPassAccelerometer(context, sensorDelay, ACCELEROMETER_LOW_PASS)
        }

        if (!Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)) {
            return GravityRotationSensor(accelerometer)
        }

        val magnetometer = LowPassMagnetometer(context, sensorDelay, MAGNETOMETER_LOW_PASS)

        return CustomGeomagneticRotationSensor(
            magnetometer,
            accelerometer,
            onlyUseMagnetometerQuality = true
        )
    }

    private fun getCustomRotationSensor(sensorDelay: Int): IOrientationSensor {
        val magnetometer = LowPassMagnetometer(context, sensorDelay, MAGNETOMETER_LOW_PASS)
        val accelerometer = LowPassAccelerometer(context, sensorDelay, ACCELEROMETER_LOW_PASS)
        val gyro = Gyroscope(context, sensorDelay)

        return CustomRotationSensor(
            magnetometer, accelerometer, gyro,
            validMagnetometerMagnitudes = Range(20f, 65f),
            validAccelerometerMagnitudes = Range(4f, 20f),
            onlyUseMagnetometerQuality = true
        )
    }

    companion object {

        const val MAGNETOMETER_LOW_PASS = 0.3f
        const val ACCELEROMETER_LOW_PASS = 0.1f

        /**
         * Returns the available compass sources in order of quality
         */
        fun getAvailableSources(context: Context): List<CompassSource> {
            val sources = mutableListOf<CompassSource>()

            if (Sensors.hasSensor(context, Sensor.TYPE_ROTATION_VECTOR)) {
                sources.add(CompassSource.RotationVector)
            }

            if (Sensors.hasSensor(context, Sensor.TYPE_GYROSCOPE) &&
                Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)
            ) {
                sources.add(CompassSource.CustomRotationVector)
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