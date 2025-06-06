package com.kylecorry.trail_sense.tools.pedometer

import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.actions.PausePedometerAction
import com.kylecorry.trail_sense.tools.pedometer.actions.ResumePedometerAction
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.DistanceAlerter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.pedometer.quickactions.QuickActionPedometer
import com.kylecorry.trail_sense.tools.pedometer.services.PedometerToolService
import com.kylecorry.trail_sense.tools.pedometer.widgets.AppWidgetPedometer
import com.kylecorry.trail_sense.tools.pedometer.widgets.PedometerToolWidgetView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory
import com.kylecorry.trail_sense.tools.tools.widgets.RefreshWidgetAction

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
            initialize = { PedometerSubsystem.getInstance(it) },
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
                    NOTIFICATION_CHANNEL_DISTANCE_ALERT,
                    context.getString(R.string.distance_alert),
                    context.getString(R.string.distance_alert),
                    Notify.CHANNEL_IMPORTANCE_HIGH,
                    false
                )
            ),
            services = listOf(PedometerToolService(context)),
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
            broadcasts = listOf(
                ToolBroadcast(
                    BROADCAST_PEDOMETER_ENABLED,
                    "Pedometer enabled"
                ),
                ToolBroadcast(
                    BROADCAST_PEDOMETER_DISABLED,
                    "Pedometer disabled"
                ),
                ToolBroadcast(
                    BROADCAST_STEPS_CHANGED,
                    "Steps changed"
                ),
                ToolBroadcast(
                    BROADCAST_DISTANCE_CHANGED,
                    "Distance changed"
                )
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
                ),
                ToolAction(
                    ACTION_REFRESH_PEDOMETER_WIDGET,
                    "Refresh widget",
                    RefreshWidgetAction(WIDGET_PEDOMETER)
                )
            ),
            widgets = listOf(
                ToolWidget(
                    WIDGET_PEDOMETER,
                    context.getString(R.string.pedometer),
                    ToolSummarySize.Half,
                    PedometerToolWidgetView(),
                    AppWidgetPedometer::class.java,
                    updateBroadcasts = listOf(BROADCAST_DISTANCE_CHANGED)
                )
            )
        )
    }

    const val BROADCAST_PEDOMETER_ENABLED = "pedometer-broadcast-pedometer-enabled"
    const val BROADCAST_PEDOMETER_DISABLED = "pedometer-broadcast-pedometer-disabled"
    const val BROADCAST_STEPS_CHANGED = "pedometer-broadcast-steps-changed"
    const val BROADCAST_DISTANCE_CHANGED = "pedometer-broadcast-distance-changed"

    const val ACTION_RESUME_PEDOMETER = "pedometer-action-resume-pedometer"
    const val ACTION_PAUSE_PEDOMETER = "pedometer-action-pause-pedometer"
    const val ACTION_REFRESH_PEDOMETER_WIDGET = "pedometer-action-refresh-pedometer-widget"

    const val SERVICE_PEDOMETER = "pedometer-service-pedometer"

    const val WIDGET_PEDOMETER = "pedometer-widget-pedometer"

    const val NOTIFICATION_CHANNEL_DISTANCE_ALERT = DistanceAlerter.NOTIFICATION_CHANNEL_ID
}