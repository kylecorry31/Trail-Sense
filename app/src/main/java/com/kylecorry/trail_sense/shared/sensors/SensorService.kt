package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.hygrometer.NullHygrometer
import com.kylecorry.trail_sense.shared.sensors.odometer.Odometer
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.sensors.speedometer.BacktrackSpeedometer
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.sensors.accelerometer.GravitySensor
import com.kylecorry.trailsensecore.infrastructure.sensors.accelerometer.IAccelerometer
import com.kylecorry.trailsensecore.infrastructure.sensors.accelerometer.LowPassAccelerometer
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.BarometricAltimeter
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.FusedAltimeter
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.IAltimeter
import com.kylecorry.trailsensecore.infrastructure.sensors.barometer.Barometer
import com.kylecorry.trailsensecore.infrastructure.sensors.barometer.IBarometer
import com.kylecorry.trailsensecore.infrastructure.sensors.compass.ICompass
import com.kylecorry.trailsensecore.infrastructure.sensors.compass.LegacyCompass
import com.kylecorry.trailsensecore.infrastructure.sensors.compass.VectorCompass
import com.kylecorry.trailsensecore.infrastructure.sensors.gps.IGPS
import com.kylecorry.trailsensecore.infrastructure.sensors.hygrometer.Hygrometer
import com.kylecorry.trailsensecore.infrastructure.sensors.hygrometer.IHygrometer
import com.kylecorry.trailsensecore.infrastructure.sensors.inclinometer.IInclinometer
import com.kylecorry.trailsensecore.infrastructure.sensors.inclinometer.Inclinometer
import com.kylecorry.trailsensecore.infrastructure.sensors.magnetometer.IMagnetometer
import com.kylecorry.trailsensecore.infrastructure.sensors.magnetometer.Magnetometer
import com.kylecorry.trailsensecore.infrastructure.sensors.network.CellSignalSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.network.ICellSignalSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.DeviceOrientation
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.DeviceOrientationSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.orientation.IOrientationSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.speedometer.ISpeedometer
import com.kylecorry.trailsensecore.infrastructure.sensors.temperature.BatteryTemperatureSensor
import com.kylecorry.trailsensecore.infrastructure.sensors.temperature.IThermometer
import com.kylecorry.trailsensecore.infrastructure.sensors.temperature.Thermometer
import com.kylecorry.trailsensecore.infrastructure.system.PermissionUtils

class SensorService(ctx: Context) {

    private var context = ctx.applicationContext
    private val userPrefs by lazy { UserPreferences(context) }
    private val sensorChecker by lazy { SensorChecker(context) }
    private val sensorManager by lazy { context.getSystemService<SensorManager>() }

    fun getGPS(background: Boolean = false): IGPS {

        val hasForegroundPermission = hasLocationPermission(false)

        if (!userPrefs.useAutoLocation || !hasForegroundPermission) {
            return OverrideGPS(context)
        }

        if (hasLocationPermission(background) && sensorChecker.hasGPS()) {
            return CustomGPS(context)
        }

        return CachedGPS(context)
    }

    private fun hasLocationPermission(background: Boolean): Boolean {
        return if (background){
            PermissionUtils.isBackgroundLocationEnabled(context)
        } else {
            PermissionUtils.isLocationEnabled(context)
        }
    }

    fun getSpeedometer(realTime: Boolean? = null): ISpeedometer {
        val useRealTime = realTime
            ?: (userPrefs.navigation.speedometerMode == NavigationPreferences.SpeedometerMode.Instantaneous)
        return if (useRealTime){
            getGPS(false)
        } else {
            BacktrackSpeedometer(context)
        }
    }

    fun getGPSAltimeter(background: Boolean = false): IAltimeter {
        val mode = userPrefs.altimeterMode

        if (mode == UserPreferences.AltimeterMode.Override) {
            return OverrideAltimeter(context)
        } else {
            if (!sensorChecker.hasGPS()) {
                return CachedAltimeter(context)
            }

            return getGPS(background)
        }
    }

    fun getAltimeter(background: Boolean = false): IAltimeter {

        val mode = userPrefs.altimeterMode

        if (mode == UserPreferences.AltimeterMode.Override) {
            return OverrideAltimeter(context)
        } else if (mode == UserPreferences.AltimeterMode.Barometer && sensorChecker.hasBarometer()) {
            return BarometricAltimeter(getBarometer()) { userPrefs.seaLevelPressureOverride }
        } else {
            if (!sensorChecker.hasGPS()) {
                return CachedAltimeter(context)
            }

            val gps = getGPS(background)

            return if (mode == UserPreferences.AltimeterMode.GPSBarometer && sensorChecker.hasBarometer()) {
                FusedAltimeter(gps, Barometer(context))
            } else {
                gps
            }
        }
    }

    fun getOdometer(): Odometer {
        return Odometer(context)
    }

    fun getCompass(): ICompass {
        val smoothing = userPrefs.navigation.compassSmoothing
        val useTrueNorth = userPrefs.navigation.useTrueNorth
        return if (userPrefs.navigation.useLegacyCompass) LegacyCompass(
            context,
            smoothing,
            useTrueNorth
        ) else VectorCompass(
            context, smoothing, useTrueNorth
        )
    }

    fun getDeviceOrientation(): DeviceOrientation {
        return DeviceOrientation(context)
    }

    fun getBarometer(): IBarometer {
        return if (userPrefs.weather.hasBarometer) Barometer(context) else NullBarometer()
    }

    fun getInclinometer(): IInclinometer {
        return Inclinometer(context)
    }

    fun getOrientationSensor(): IOrientationSensor {
        return DeviceOrientationSensor(context)
    }

    @Suppress("DEPRECATION")
    fun getThermometer(): IThermometer {
        if (sensorChecker.hasSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)) {
            return Thermometer(context, Sensor.TYPE_AMBIENT_TEMPERATURE)
        }

        if (sensorChecker.hasSensor(Sensor.TYPE_TEMPERATURE)) {
            return Thermometer(context, Sensor.TYPE_TEMPERATURE)
        }

        val builtInSensors = sensorManager?.getSensorList(Sensor.TYPE_ALL) ?: listOf()

        val first = builtInSensors.filter {
            it.name.contains("temperature", true) ||
                    it.name.contains("thermometer", true)
        }.minBy { it.resolution }

        if (first != null) {
            return Thermometer(context, first.type)
        }

        return BatteryTemperatureSensor(context)
    }

    fun getHygrometer(): IHygrometer {
        if (sensorChecker.hasHygrometer()) {
            return Hygrometer(context)
        }

        return NullHygrometer()
    }

    fun getCellSignal(background: Boolean = false): ICellSignalSensor {
        if (!hasLocationPermission(background)){
            return NullCellSignalSensor()
        }
        return CellSignalSensor(context, userPrefs.cellSignal.populateCache)
    }

    fun getGravity(): IAccelerometer {
        return if (sensorChecker.hasSensor(Sensor.TYPE_GRAVITY)){
            GravitySensor(context)
        } else {
            LowPassAccelerometer(context)
        }
    }

    fun getMagnetometer(): IMagnetometer {
        return Magnetometer(context)
    }

}