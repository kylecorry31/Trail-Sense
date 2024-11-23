package com.kylecorry.trail_sense.settings

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.receivers.ServiceRestartAlerter
import com.kylecorry.trail_sense.settings.backup.AutomaticBackupDiagnosticScanner
import com.kylecorry.trail_sense.settings.backup.BackupFailedAlerter
import com.kylecorry.trail_sense.settings.backup.BackupToolService
import com.kylecorry.trail_sense.settings.quickactions.QuickActionSettings
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnostic
import com.kylecorry.trail_sense.tools.tools.quickactions.QuickActionToolWidgets
import com.kylecorry.trail_sense.tools.tools.services.WidgetUpdateToolService

object SettingsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.SETTINGS,
            context.getString(R.string.settings),
            R.drawable.ic_settings,
            R.id.action_settings,
            ToolCategory.Other,
            guideId = R.raw.guide_tool_settings,
            additionalNavigationIds = listOf(
                R.id.unitSettingsFragment,
                R.id.privacySettingsFragment,
                R.id.experimentalSettingsFragment,
                R.id.errorSettingsFragment,
                R.id.sensorSettingsFragment,
                R.id.licenseFragment,
                R.id.cellSignalSettingsFragment,
                R.id.calibrateCompassFragment,
                R.id.calibrateAltimeterFragment,
                R.id.calibrateGPSFragment,
                R.id.calibrateBarometerFragment,
                R.id.thermometerSettingsFragment,
                R.id.cameraSettingsFragment,
                R.id.toolsSettingsFragment,
                // TODO: Add all the tool settings
            ),
            notificationChannels = listOf(
                ToolNotificationChannel(
                    ServiceRestartAlerter.CHANNEL_SERVICE_RESTART,
                    context.getString(R.string.service_restart),
                    context.getString(R.string.service_restart_channel_description),
                    Notify.CHANNEL_IMPORTANCE_LOW,
                    true
                ),
                ToolNotificationChannel(
                    BackupFailedAlerter.CHANNEL_BACKUP_FAILED,
                    context.getString(R.string.backup_failed),
                    context.getString(R.string.backup_failed),
                    Notify.CHANNEL_IMPORTANCE_LOW,
                    true
                )
            ),
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_SETTINGS,
                    context.getString(R.string.settings),
                    ::QuickActionSettings
                ),
                ToolQuickAction(
                    Tools.QUICK_ACTION_TOOL_WIDGETS,
                    context.getString(R.string.widgets),
                    ::QuickActionToolWidgets
                )
            ),
            services = listOf(
                BackupToolService(context),
                WidgetUpdateToolService(context)
            ),
            diagnostics = listOf(
                ToolDiagnostic(
                    "settings-auto-backup",
                    context.getString(R.string.automatic_backup),
                    scanner = AutomaticBackupDiagnosticScanner()
                )
            ),
            broadcasts = listOf(
                ToolBroadcast(
                    BROADCAST_UPDATE_IN_APP_WIDGET,
                    "Update in-app widget"
                )
            )
        )
    }

    const val SERVICE_AUTO_BACKUP = "settings-service-auto-backup"
    const val SERVICE_WIDGET_UPDATER = "settings-service-widget-updater"

    const val BROADCAST_UPDATE_IN_APP_WIDGET = "settings-broadcast-update-in-app-widget"
}