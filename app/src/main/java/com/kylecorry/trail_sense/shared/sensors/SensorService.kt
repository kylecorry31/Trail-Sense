package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.declination.AutoDeclinationProvider
import com.kylecorry.trail_sense.shared.sensors.declination.IDeclinationProvider
import com.kylecorry.trail_sense.shared.sensors.declination.OverrideDeclination
import com.kylecorry.trail_sense.shared.sensors.hygrometer.Hygrometer
import com.kylecorry.trail_sense.shared.sensors.hygrometer.IHygrometer
import com.kylecorry.trail_sense.shared.sensors.hygrometer.NullHygrometer
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.sensors.temperature.*

class SensorService(private val context: Context) {

    private val userPrefs = UserPreferences(context)
    private val sensorChecker = SensorChecker(context)
    private val sensorManager = context.getSystemService<SensorManager>()

    fun getGPS(): IGPS {
        if (!userPrefs.useAutoLocation) {
            return OverrideGPS(context)
        }

        if (userPrefs.useLocationFeatures) {
            return GPS(context)
        }

        return CachedGPS(context)
    }

    fun getAltimeter(): IAltimeter {
        if (!userPrefs.useAutoAltitude) {
            return OverrideAltimeter(context)
        }

        if (!userPrefs.useLocationFeatures) {
            return CachedAltimeter(context)
        }

        val gps = getGPS()

        return if (userPrefs.useFineTuneAltitude && userPrefs.weather.hasBarometer) {
            FusedAltimeter(gps, Barometer(context))
        } else {
            gps
        }
    }

    fun getDeclinationProvider(): IDeclinationProvider {
        if (!userPrefs.useAutoDeclination) {
            return OverrideDeclination(context)
        }

        val gps = getGPS()
        val altimeter = getAltimeter()

        return AutoDeclinationProvider(gps, altimeter)
    }

    fun getCompass(): ICompass {
        return if (userPrefs.navigation.useLegacyCompass) LegacyCompass(context) else VectorCompass(
            context
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

}