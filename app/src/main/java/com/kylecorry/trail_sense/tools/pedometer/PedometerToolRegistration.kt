package com.kylecorry.trail_sense.tools.pedometer

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.DistanceAlerter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.pedometer.quickactions.QuickActionPedometer
import com.kylecorry.trail_sense.tools.pedometer.receivers.PausePedometerAction
import com.kylecorry.trail_sense.tools.pedometer.receivers.ResumePedometerAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory
import java.time.Duration

object PedometerToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.PEDOMETER,
            context.getString(R.string.pedometer),
            R.drawable.steps,
            R.id.fragmentToolPedometer,
            ToolCategory.Distance,
            guideId = R.raw.guide_tool_pedometer,
            settingsNavAction = R.id.calibrateOdometerFragment,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_PEDOMETER,
                    context.getString(R.string.pedometer),
                    ::QuickActionPedometer
                )
            ),
            isAvailable = { Sensors.hasSensor(it, Sensor.TYPE_STEP_COUNTER) },
            tiles = listOf(
                "com.kylecorry.trail_sense.tools.pedometer.tiles.PedometerTile"
            ),
            notificationChannels = listOf(
                ToolNotificationChannel(
                    StepCounterService.CHANNEL_ID,
                    context.getString(R.string.pedometer),
                    context.getString(R.string.pedometer),
                    Notify.CHANNEL_IMPORTANCE_LOW,
                    true
                ),
                // This may be shared at some point
                ToolNotificationChannel(
                    DistanceAlerter.NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.distance_alert),
                    context.getString(R.string.distance_alert),
                    Notify.CHANNEL_IMPORTANCE_HIGH,
                    false
                )
            ),
            services = listOf(
                ToolService(
                    context.getString(R.string.pedometer),
                    getFrequency = { Duration.ZERO },
                    isActive = {
                        val prefs = UserPreferences(it)
                        prefs.pedometer.isEnabled && !prefs.isLowPowerModeOn
                    },
                    disable = {
                        UserPreferences(it).pedometer.isEnabled = false
                        StepCounterService.stop(it)
                    },
                    stop = {
                        StepCounterService.stop(it)
                    },
                    restart = {
                        val prefs = UserPreferences(it)
                        if (prefs.pedometer.isEnabled) {
                            StepCounterService.start(it)
                        } else {
                            StepCounterService.stop(it)
                        }
                    }
                )
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.pedometer(context),
                ToolDiagnosticFactory.gps(context),
                ToolDiagnosticFactory.notification(
                    StepCounterService.CHANNEL_ID,
                    context.getString(R.string.pedometer),
                ),
                ToolDiagnosticFactory.notification(
                    DistanceAlerter.NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.distance_alert)
                ),
                ToolDiagnosticFactory.powerSaver(context),
                ToolDiagnosticFactory.backgroundService(context)
            ),
            actions = listOf(
                ToolAction(
                    ACTION_RESUME_PEDOMETER,
                    "Resume pedometer",
                    ResumePedometerAction()
                ),
                ToolAction(
                    ACTION_PAUSE_PEDOMETER,
                    "Pause pedometer",
                    PausePedometerAction()
                )
            )
        )
    }

    const val ACTION_RESUME_PEDOMETER =
        "com.kylecorry.trail_sense.tools.pedometer.ACTION_RESUME_PEDOMETER"
    const val ACTION_PAUSE_PEDOMETER =
        "com.kylecorry.trail_sense.tools.pedometer.ACTION_PAUSE_PEDOMETER"
}