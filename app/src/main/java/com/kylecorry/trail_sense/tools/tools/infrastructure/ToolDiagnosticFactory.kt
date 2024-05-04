package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import com.kylecorry.luna.text.slugify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.AccelerometerDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.AlarmDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.AltimeterDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.BackgroundLocationDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.BarometerDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.BatteryDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.CameraDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.FlashlightDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.GPSDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.GyroscopeDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.LightSensorDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.MagnetometerDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.NotificationDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.PedometerDiagnosticScanner

object ToolDiagnosticFactory {

    fun gps(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "gps",
            context.getString(R.string.gps),
            scanner = GPSDiagnosticScanner()
        )
    }

    fun backgroundLocation(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "background-location",
            context.getString(R.string.background_location_permission),
            scanner = BackgroundLocationDiagnosticScanner()
        )
    }

    fun alarm(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "alarm",
            context.getString(R.string.permission_alarms_and_reminders),
            scanner = AlarmDiagnosticScanner()
        )
    }

    fun notification(channelId: String, channelName: String): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "notification-${channelId.slugify()}",
            channelName,
            scanner = NotificationDiagnosticScanner(channelId, channelName)
        )
    }

    fun accelerometer(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "accelerometer",
            context.getString(R.string.gravity),
            scanner = AccelerometerDiagnosticScanner()
        )
    }

    fun altimeterOverride(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "altimeter",
            context.getString(R.string.pref_altimeter_calibration_title),
            scanner = AltimeterDiagnosticScanner()
        )
    }

    fun barometer(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "barometer",
            context.getString(R.string.barometer),
            scanner = BarometerDiagnosticScanner()
        )
    }

    fun battery(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "battery",
            context.getString(R.string.tool_battery_title),
            scanner = BatteryDiagnosticScanner()
        )
    }

    fun camera(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "camera",
            context.getString(R.string.camera),
            scanner = CameraDiagnosticScanner()
        )
    }

    fun flashlight(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "flashlight",
            context.getString(R.string.flashlight_title),
            scanner = FlashlightDiagnosticScanner()
        )
    }

    fun gyroscope(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "gyroscope",
            context.getString(R.string.sensor_gyroscope),
            scanner = GyroscopeDiagnosticScanner()
        )
    }

    fun light(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "light-sensor",
            context.getString(R.string.tool_light_meter_title),
            scanner = LightSensorDiagnosticScanner()
        )
    }

    fun magnetometer(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "magnetometer",
            context.getString(R.string.magnetometer),
            scanner = MagnetometerDiagnosticScanner()
        )
    }

    fun pedometer(context: Context): ToolDiagnostic2 {
        return ToolDiagnostic2(
            "pedometer",
            context.getString(R.string.pedometer),
            scanner = PedometerDiagnosticScanner()
        )
    }

    // Higher level
    fun compass(context: Context): Array<ToolDiagnostic2> {
        return arrayOf(
            accelerometer(context),
            magnetometer(context),
            gyroscope(context)
        )
    }

    fun sightingCompass(context: Context): Array<ToolDiagnostic2> {
        return arrayOf(
            *compass(context),
            camera(context)
        )
    }

    fun tilt(context: Context): Array<ToolDiagnostic2> {
        return arrayOf(
            accelerometer(context),
            gyroscope(context)
        )
    }

    fun altimeter(context: Context): Array<ToolDiagnostic2> {
        return arrayOf(
            barometer(context),
            gps(context),
            altimeterOverride(context)
        )
    }

}