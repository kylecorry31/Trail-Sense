package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.andromeda.core.sensors.IThermometer
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.accelerometer.GravitySensor
import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import com.kylecorry.andromeda.sense.accelerometer.LowPassAccelerometer
import com.kylecorry.andromeda.sense.barometer.Barometer
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.hygrometer.Hygrometer
import com.kylecorry.andromeda.sense.hygrometer.IHygrometer
import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.andromeda.sense.orientation.GameRotationSensor
import com.kylecorry.andromeda.sense.orientation.Gyroscope
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.andromeda.sense.pedometer.IPedometer
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.andromeda.sense.temperature.AmbientThermometer
import com.kylecorry.andromeda.sense.temperature.Thermometer
import com.kylecorry.andromeda.signal.CellSignalSensor
import com.kylecorry.andromeda.signal.ICellSignalSensor
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.altimeter.*
import com.kylecorry.trail_sense.shared.sensors.hygrometer.NullHygrometer
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.sensors.providers.CompassProvider
import com.kylecorry.trail_sense.shared.sensors.speedometer.BacktrackSpeedometer
import com.kylecorry.trail_sense.shared.sensors.thermometer.CalibratedThermometerWrapper
import com.kylecorry.trail_sense.shared.sensors.thermometer.HistoricThermometer
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.AveragePaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.CurrentPaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import java.time.Duration

// Maybe use the concept of a use case
// Ex. SensorPurpose.Background, SensorPurpose.Calibration, SensorPurpose.Diagnostics
// Using those, it can adjust settings to be more appropriate for the use case

class SensorService(ctx: Context) {

    private var context = ctx.applicationContext
    private val userPrefs by lazy { UserPreferences(context) }

    fun getGPS(frequency: Duration = Duration.ofMillis(20)): IGPS {

        val hasPermission = hasLocationPermission()

        if (!userPrefs.useAutoLocation || !hasPermission) {
            return OverrideGPS(context, frequency.toMillis())
        }

        if (GPS.isAvailable(context)) {
            return CustomGPS(context, frequency)
        }

        return CachedGPS(context, frequency.toMillis())
    }

    fun getGPSFromAltimeter(altimeter: IAltimeter): IGPS? {
        return if (altimeter is IGPS) {
            altimeter
        } else if (altimeter is AltimeterWrapper && altimeter.altimeter is IGPS) {
            altimeter.altimeter as IGPS
        } else if (altimeter is AltimeterWrapper && altimeter.altimeter is AltimeterWrapper) {
            getGPSFromAltimeter(altimeter.altimeter)
        } else {
            null
        }
    }

    fun hasLocationPermission(background: Boolean = false): Boolean {
        return if (background) {
            Permissions.isBackgroundLocationEnabled(context)
        } else {
            Permissions.canGetFineLocation(context)
        }
    }

    fun getPedometer(): IPedometer {
        return if (Permissions.canRecognizeActivity(context)) {
            Pedometer(context, ENVIRONMENT_SENSOR_DELAY)
        } else {
            NullPedometer()
        }
    }

    fun getSpeedometer(gps: IGPS? = null): ISpeedometer {
        return when (userPrefs.navigation.speedometerMode) {
            NavigationPreferences.SpeedometerMode.Backtrack -> BacktrackSpeedometer(context)
            NavigationPreferences.SpeedometerMode.GPS -> gps ?: getGPS()
            NavigationPreferences.SpeedometerMode.CurrentPace -> CurrentPaceSpeedometer(
                getPedometer(), StrideLengthPaceCalculator(userPrefs.pedometer.strideLength)
            )

            NavigationPreferences.SpeedometerMode.AveragePace -> AveragePaceSpeedometer(
                StepCounter(
                    PreferencesSubsystem.getInstance(context).preferences
                ), StrideLengthPaceCalculator(userPrefs.pedometer.strideLength)
            )
        }
    }

    private fun getGPSAltimeter(gps: IGPS? = null): IAltimeter {
        val mode = userPrefs.altimeterMode

        if (mode == UserPreferences.AltimeterMode.Override) {
            return OverrideAltimeter(context)
        } else {
            if (!GPS.isAvailable(context)) {
                return CachedAltimeter(context)
            }

            return gps ?: getGPS()
        }
    }

    fun getAltimeter(
        preferGPS: Boolean = false,
        gps: IGPS? = null
    ): IAltimeter {
        if (preferGPS) {
            return CachingAltimeterWrapper(
                context,
                GaussianAltimeterWrapper(
                    getGPSAltimeter(gps),
                    userPrefs.altimeterSamples
                )
            )
        }

        val mode = userPrefs.altimeterMode

        if (mode == UserPreferences.AltimeterMode.Override) {
            return OverrideAltimeter(context)
        } else if (mode == UserPreferences.AltimeterMode.Barometer && Sensors.hasBarometer(
                context
            )
        ) {
            return CachingAltimeterWrapper(
                context,
                Barometer(
                    context,
                    ENVIRONMENT_SENSOR_DELAY,
                    seaLevelPressure = userPrefs.seaLevelPressureOverride
                )
            )
        } else {
            if (!GPS.isAvailable(context)) {
                return CachedAltimeter(context)
            }

            val gps = gps ?: getGPS()

            return if (mode == UserPreferences.AltimeterMode.GPSBarometer && Sensors.hasBarometer(
                    context
                )
            ) {
                CachingAltimeterWrapper(
                    context,
                    FusedAltimeter(gps, Barometer(context))
                )
            } else {
                CachingAltimeterWrapper(
                    context,
                    GaussianAltimeterWrapper(gps, userPrefs.altimeterSamples),
                )
            }
        }
    }

    fun getCompass(): ICompass {
        return CompassProvider(context, userPrefs.compass).get()
    }

    fun getDeviceOrientationSensor(): DeviceOrientation {
        // While not technically an environment sensor, it doesn't need to update often - and can match their rate
        return DeviceOrientation(context, ENVIRONMENT_SENSOR_DELAY)
    }

    fun getBarometer(): IBarometer {
        return if (userPrefs.weather.hasBarometer) Barometer(
            context,
            ENVIRONMENT_SENSOR_DELAY
        ) else NullBarometer()
    }

    fun getThermometer(calibrated: Boolean = true): IThermometer {
        val thermometer = when (userPrefs.thermometer.source) {
            ThermometerSource.Historic -> HistoricThermometer(context)
            ThermometerSource.Sensor -> getThermometerSensor()
        }
        return if (calibrated) {
            CalibratedThermometerWrapper(
                thermometer,
                userPrefs.thermometer.calibrator
            )
        } else {
            thermometer
        }
    }

    @Suppress("DEPRECATION")
    private fun getThermometerSensor(): IThermometer {
        if (Sensors.hasSensor(context, Sensor.TYPE_AMBIENT_TEMPERATURE)) {
            return AmbientThermometer(context, ENVIRONMENT_SENSOR_DELAY)
        }

        if (Sensors.hasSensor(context, Sensor.TYPE_TEMPERATURE)) {
            return Thermometer(context, ENVIRONMENT_SENSOR_DELAY)
        }

        return Battery(context)
    }

    fun getHygrometer(): IHygrometer {
        if (Sensors.hasHygrometer(context)) {
            return Hygrometer(context, ENVIRONMENT_SENSOR_DELAY)
        }

        return NullHygrometer()
    }

    fun getCellSignal(): ICellSignalSensor {
        if (!hasLocationPermission()) {
            return NullCellSignalSensor()
        }
        return CellSignalSensor(context, userPrefs.cellSignal.populateCache)
    }

    fun getGravity(): IAccelerometer {
        return if (Sensors.hasSensor(context, Sensor.TYPE_GRAVITY)) {
            GravitySensor(context, MOTION_SENSOR_DELAY)
        } else {
            LowPassAccelerometer(context, MOTION_SENSOR_DELAY)
        }
    }

    fun getMagnetometer(): IMagnetometer {
        return Magnetometer(context, MOTION_SENSOR_DELAY)
    }

    fun getGyroscope(): IOrientationSensor {
        if (!Sensors.hasGyroscope(context)) {
            return NullGyroscope()
        }
        if (Sensors.hasSensor(context, Sensor.TYPE_GAME_ROTATION_VECTOR)) {
            return GameRotationSensor(context, MOTION_SENSOR_DELAY)
        }
        return Gyroscope(context, MOTION_SENSOR_DELAY)
    }

    companion object {
        const val MOTION_SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME
        const val ENVIRONMENT_SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL
    }

}