package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.andromeda.core.sensors.IThermometer
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.accelerometer.GravitySensor
import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import com.kylecorry.andromeda.sense.accelerometer.LowPassAccelerometer
import com.kylecorry.andromeda.sense.barometer.Barometer
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.andromeda.sense.compass.GravityCompensatedCompass
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.compass.LegacyCompass
import com.kylecorry.andromeda.sense.hygrometer.Hygrometer
import com.kylecorry.andromeda.sense.hygrometer.IHygrometer
import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.andromeda.sense.orientation.Gyroscope
import com.kylecorry.andromeda.sense.orientation.IGyroscope
import com.kylecorry.andromeda.sense.pedometer.IPedometer
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.andromeda.sense.temperature.AmbientThermometer
import com.kylecorry.andromeda.sense.temperature.Thermometer
import com.kylecorry.andromeda.signal.CellSignalSensor
import com.kylecorry.andromeda.signal.ICellSignalSensor
import com.kylecorry.sol.math.filters.MovingAverageFilter
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.altimeter.FilteredAltimeter
import com.kylecorry.trail_sense.shared.sensors.altimeter.FusedAltimeter
import com.kylecorry.trail_sense.shared.sensors.altimeter.MedianAltimeter
import com.kylecorry.trail_sense.shared.sensors.hygrometer.NullHygrometer
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.sensors.speedometer.BacktrackSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.AveragePaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.CurrentPaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounter
import java.time.Duration
import kotlin.math.max

class SensorService(ctx: Context) {

    private var context = ctx.applicationContext
    private val userPrefs by lazy { UserPreferences(context) }

    fun getGPS(background: Boolean = false, frequency: Duration = Duration.ofMillis(20)): IGPS {

        val hasForegroundPermission = hasLocationPermission(false)

        if (!userPrefs.useAutoLocation || !hasForegroundPermission) {
            return OverrideGPS(context, frequency.toMillis())
        }

        if (hasLocationPermission(background) && GPS.isAvailable(context)) {
            return CustomGPS(context, frequency)
        }

        return CachedGPS(context, frequency.toMillis())
    }

    fun getGPSFromAltimeter(altimeter: IAltimeter): IGPS? {
        return if (altimeter is IGPS) {
            altimeter
        } else if (altimeter is FilteredAltimeter && altimeter.altimeter is IGPS) {
            altimeter.altimeter as IGPS
        } else {
            null
        }
    }

    private fun hasLocationPermission(background: Boolean): Boolean {
        return if (background) {
            Permissions.isBackgroundLocationEnabled(context)
        } else {
            Permissions.canGetFineLocation(context)
        }
    }

    fun getPedometer(): IPedometer {
        return if (Permissions.canRecognizeActivity(context)) {
            Pedometer(context)
        } else {
            NullPedometer()
        }
    }

    fun getSpeedometer(): ISpeedometer {
        return when (userPrefs.navigation.speedometerMode) {
            NavigationPreferences.SpeedometerMode.Backtrack -> BacktrackSpeedometer(context)
            NavigationPreferences.SpeedometerMode.GPS -> getGPS(false)
            NavigationPreferences.SpeedometerMode.CurrentPace -> CurrentPaceSpeedometer(
                getPedometer(), StrideLengthPaceCalculator(userPrefs.pedometer.strideLength)
            )
            NavigationPreferences.SpeedometerMode.AveragePace -> AveragePaceSpeedometer(
                StepCounter(
                    Preferences(context)
                ), StrideLengthPaceCalculator(userPrefs.pedometer.strideLength)
            )
        }
    }

    private fun getGPSAltimeter(background: Boolean = false): IAltimeter {
        val mode = userPrefs.altimeterMode

        if (mode == UserPreferences.AltimeterMode.Override) {
            return OverrideAltimeter(context)
        } else {
            if (!GPS.isAvailable(context)) {
                return CachedAltimeter(context)
            }

            return getGPS(background)
        }
    }

    fun getAltimeter(background: Boolean = false, preferGPS: Boolean = false): IAltimeter {
        if (preferGPS) {
            return MedianAltimeter(getGPSAltimeter(background), userPrefs.altimeterSamples)
        }

        val mode = userPrefs.altimeterMode

        if (mode == UserPreferences.AltimeterMode.Override) {
            return OverrideAltimeter(context)
        } else if (mode == UserPreferences.AltimeterMode.Barometer && Sensors.hasBarometer(
                context
            )
        ) {
            return Barometer(context, seaLevelPressure = userPrefs.seaLevelPressureOverride)
        } else {
            if (!GPS.isAvailable(context)) {
                return CachedAltimeter(context)
            }

            val gps = getGPS(background)

            return if (mode == UserPreferences.AltimeterMode.GPSBarometer && Sensors.hasBarometer(
                    context
                )
            ) {
                FusedAltimeter(gps, Barometer(context))
            } else {
                MedianAltimeter(gps, userPrefs.altimeterSamples)
            }
        }
    }

    fun getCompass(): ICompass {
        val smoothing = userPrefs.navigation.compassSmoothing
        val useTrueNorth = userPrefs.navigation.useTrueNorth

        return if (userPrefs.navigation.useLegacyCompass) LegacyCompass(
            context,
            useTrueNorth,
            MovingAverageFilter(max(1, smoothing * 2))
        ) else GravityCompensatedCompass(
            context, useTrueNorth, MovingAverageFilter(max(1, smoothing * 4))
        )
    }

    fun getDeviceOrientationSensor(): DeviceOrientation {
        return DeviceOrientation(context)
    }

    fun getBarometer(): IBarometer {
        return if (userPrefs.weather.hasBarometer) Barometer(context) else NullBarometer()
    }

    @Suppress("DEPRECATION")
    fun getThermometer(): IThermometer {
        if (Sensors.hasSensor(context, Sensor.TYPE_AMBIENT_TEMPERATURE)) {
            return AmbientThermometer(context)
        }

        if (Sensors.hasSensor(context, Sensor.TYPE_TEMPERATURE)) {
            return Thermometer(context)
        }

        return Battery(context)
    }

    fun getHygrometer(): IHygrometer {
        if (Sensors.hasHygrometer(context)) {
            return Hygrometer(context)
        }

        return NullHygrometer()
    }

    fun getCellSignal(background: Boolean = false): ICellSignalSensor {
        if (!hasLocationPermission(background)) {
            return NullCellSignalSensor()
        }
        return CellSignalSensor(context, userPrefs.cellSignal.populateCache)
    }

    fun getGravity(): IAccelerometer {
        return if (Sensors.hasSensor(context, Sensor.TYPE_GRAVITY)) {
            GravitySensor(context)
        } else {
            LowPassAccelerometer(context)
        }
    }

    fun getMagnetometer(): IMagnetometer {
        return Magnetometer(context)
    }

    fun getGyroscope(): IGyroscope {
        if (!Sensors.hasGyroscope(context)) {
            return NullGyroscope()
        }
        return Gyroscope(context)
    }

}