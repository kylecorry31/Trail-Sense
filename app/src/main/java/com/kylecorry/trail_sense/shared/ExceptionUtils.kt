package com.kylecorry.trail_sense.shared

import android.content.Context
import android.hardware.Sensor
import android.os.Build
import com.kylecorry.andromeda.core.system.Android
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.Sensors

object ExceptionUtils {

    fun report(context: Context, throwable: Throwable?, email: String, appName: String) {
        val androidVersion = Build.VERSION.SDK_INT
        val device = "${Android.fullDeviceName} (${Android.model})"
        val appVersion = Package.getVersionName(context)
        val message = throwable?.message ?: ""
        val stackTrace = throwable?.stackTraceToString() ?: ""
        var sensors = ""
        try {
            sensors = getSensorDetails(context)
        } catch (e: Exception) {
            // Don't do anything
        }

        val body =
            "Version: ${appVersion}\nDevice: ${device}\nAndroid SDK: ${androidVersion}\nSensors\n$sensors\n\nMessage: ${message}\n\n$stackTrace"

        val intent = Intents.email(
            email,
            "Error in $appName $appVersion",
            body
        )
        context.startActivity(intent)
    }

    @Suppress("DEPRECATION")
    private fun getSensorDetails(context: Context): String {
        val locationPermission = Permissions.canGetFineLocation(context)
        val backgroundLocationPermission = Permissions.isBackgroundLocationEnabled(context)
        val gps = GPS.isAvailable(context)
        val barometer = Sensors.hasBarometer(context)
        val gravity =  Sensors.hasGravity(context)
        val hygrometer = Sensors.hasHygrometer(context)
        val magnetometer = Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)
        val accelerometer = Sensors.hasSensor(context, Sensor.TYPE_ACCELEROMETER)
        val orientation = Sensors.hasSensor(context, Sensor.TYPE_ORIENTATION)

        return "Location: $locationPermission\nBackground Location: $backgroundLocationPermission\nGPS: $gps\nBarometer: $barometer\nGravity: $gravity\nHygrometer: $hygrometer\nMagnetometer: $magnetometer\nAccelerometer: $accelerometer\nOrientation: $orientation"
    }

}