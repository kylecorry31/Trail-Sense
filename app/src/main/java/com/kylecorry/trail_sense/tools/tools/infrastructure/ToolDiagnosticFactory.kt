package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import com.kylecorry.luna.text.slugify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.AccelerometerDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.AlarmDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.BackgroundLocationDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.GPSDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.NotificationDiagnosticScanner

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

}