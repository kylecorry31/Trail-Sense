package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import com.kylecorry.luna.text.slugify
import com.kylecorry.trail_sense.R

object ToolDiagnosticFactory {

    fun gps(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "gps",
            context.getString(R.string.gps),
            scanner = GPSDiagnosticScanner()
        )
    }

    fun backgroundLocation(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "background-location",
            context.getString(R.string.background_location_permission),
            scanner = BackgroundLocationDiagnosticScanner()
        )
    }

    fun backgroundService(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "background-service",
            context.getString(R.string.background),
            scanner = BackgroundServiceDiagnosticScanner()
        )
    }

    fun powerSaver(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "power-saver",
            context.getString(R.string.pref_low_power_mode_title),
            scanner = PowerSaverDiagnosticScanner()
        )
    }

    fun alarm(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "alarm",
            context.getString(R.string.permission_alarms_and_reminders),
            scanner = AlarmDiagnosticScanner()
        )
    }

    fun notification(channelId: String, channelName: String): ToolDiagnostic {
        return ToolDiagnostic(
            "notification-${channelId.slugify()}",
            channelName,
            scanner = NotificationDiagnosticScanner(channelId, channelName)
        )
    }

    fun accelerometer(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "accelerometer",
            context.getString(R.string.gravity),
            scanner = AccelerometerDiagnosticScanner()
        )
    }

    fun altimeterOverride(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "altimeter",
            context.getString(R.string.pref_altimeter_calibration_title),
            scanner = AltimeterDiagnosticScanner()
        )
    }

    fun barometer(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "barometer",
            context.getString(R.string.barometer),
            scanner = BarometerDiagnosticScanner()
        )
    }

    fun battery(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "battery",
            context.getString(R.string.tool_battery_title),
            scanner = BatteryDiagnosticScanner()
        )
    }

    fun camera(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "camera",
            context.getString(R.string.camera),
            scanner = CameraDiagnosticScanner()
        )
    }

    fun flashlight(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "flashlight",
            context.getString(R.string.flashlight_title),
            scanner = FlashlightDiagnosticScanner()
        )
    }

    fun gyroscope(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "gyroscope",
            context.getString(R.string.sensor_gyroscope),
            scanner = GyroscopeDiagnosticScanner()
        )
    }

    fun light(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "light-sensor",
            context.getString(R.string.tool_light_meter_title),
            scanner = LightSensorDiagnosticScanner()
        )
    }

    fun magnetometer(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "magnetometer",
            context.getString(R.string.magnetometer),
            scanner = MagnetometerDiagnosticScanner()
        )
    }

    fun pedometer(context: Context): ToolDiagnostic {
        return ToolDiagnostic(
            "pedometer",
            context.getString(R.string.pedometer),
            scanner = PedometerDiagnosticScanner()
        )
    }

    // Higher level
    fun compass(context: Context): Array<ToolDiagnostic> {
        return arrayOf(
            accelerometer(context),
            magnetometer(context),
            gyroscope(context)
        )
    }

    fun sightingCompass(context: Context): Array<ToolDiagnostic> {
        return arrayOf(
            *compass(context),
            camera(context)
        )
    }

    fun tilt(context: Context): Array<ToolDiagnostic> {
        return arrayOf(
            accelerometer(context),
            gyroscope(context)
        )
    }

    fun altimeter(context: Context): Array<ToolDiagnostic> {
        return arrayOf(
            barometer(context),
            gps(context),
            altimeterOverride(context)
        )
    }

}