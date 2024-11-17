package com.kylecorry.trail_sense.tools.paths

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.paths.actions.PauseBacktrackAction
import com.kylecorry.trail_sense.tools.paths.actions.ResumeBacktrackAction
import com.kylecorry.trail_sense.tools.paths.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.tools.paths.quickactions.QuickActionBacktrack
import com.kylecorry.trail_sense.tools.paths.services.BacktrackToolService
import com.kylecorry.trail_sense.tools.paths.widgets.AppWidgetBacktrack
import com.kylecorry.trail_sense.tools.paths.widgets.BacktrackToolWidgetView
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

object PathsToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.PATHS,
            context.getString(R.string.paths),
            R.drawable.ic_tool_backtrack,
            R.id.fragmentBacktrack,
            ToolCategory.Location,
            guideId = R.raw.guide_tool_paths,
            settingsNavAction = R.id.pathsSettingsFragment,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_BACKTRACK,
                    context.getString(R.string.backtrack),
                    ::QuickActionBacktrack
                )
            ),
            additionalNavigationIds = listOf(
                R.id.pathDetailsFragment
            ),
            tiles = listOf(
                "com.kylecorry.trail_sense.tools.paths.tiles.BacktrackTile"
            ),
            widgets = listOf(
                ToolWidget(
                    WIDGET_BACKTRACK,
                    context.getString(R.string.backtrack),
                    ToolSummarySize.Half,
                    BacktrackToolWidgetView(),
                    AppWidgetBacktrack::class.java,
                    updateBroadcasts = listOf(
                        BROADCAST_PATHS_CHANGED,
                        BROADCAST_BACKTRACK_STATE_CHANGED
                    )
                )
            ),
            notificationChannels = listOf(
                ToolNotificationChannel(
                    BacktrackService.FOREGROUND_CHANNEL_ID,
                    context.getString(R.string.backtrack),
                    context.getString(R.string.backtrack_notification_channel_description),
                    Notify.CHANNEL_IMPORTANCE_LOW,
                    muteSound = true
                )
            ),
            services = listOf(BacktrackToolService(context)),
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.altimeter(context),
                *ToolDiagnosticFactory.compass(context),
                ToolDiagnosticFactory.backgroundLocation(context),
                ToolDiagnosticFactory.notification(
                    BacktrackService.FOREGROUND_CHANNEL_ID,
                    context.getString(R.string.backtrack)
                ),
                ToolDiagnosticFactory.powerSaver(context),
                ToolDiagnosticFactory.backgroundService(context)
            ).distinctBy { it.id },
            broadcasts = listOf(
                ToolBroadcast(
                    BROADCAST_BACKTRACK_ENABLED,
                    "Backtrack enabled"
                ),
                ToolBroadcast(
                    BROADCAST_BACKTRACK_DISABLED,
                    "Backtrack disabled"
                ),
                ToolBroadcast(
                    BROADCAST_BACKTRACK_STATE_CHANGED,
                    "Backtrack state changed"
                ),
                ToolBroadcast(
                    BROADCAST_BACKTRACK_FREQUENCY_CHANGED,
                    "Backtrack frequency changed"
                ),
                ToolBroadcast(
                    BROADCAST_PATHS_CHANGED,
                    "Paths changed"
                )
            ),
            actions = listOf(
                ToolAction(
                    ACTION_RESUME_BACKTRACK,
                    "Resume backtrack",
                    ResumeBacktrackAction()
                ),
                ToolAction(
                    ACTION_PAUSE_BACKTRACK,
                    "Pause backtrack",
                    PauseBacktrackAction()
                )
            )
        )
    }

    const val BROADCAST_BACKTRACK_ENABLED = "paths-broadcast-backtrack-enabled"
    const val BROADCAST_BACKTRACK_DISABLED = "paths-broadcast-backtrack-disabled"
    const val BROADCAST_BACKTRACK_STATE_CHANGED = "paths-broadcast-backtrack-state-changed"
    const val BROADCAST_BACKTRACK_FREQUENCY_CHANGED = "paths-broadcast-backtrack-frequency-changed"
    const val BROADCAST_PATHS_CHANGED = "paths-broadcast-backtrack-paths-changed"

    const val BROADCAST_PARAM_BACKTRACK_FREQUENCY = "paths-broadcast-param-backtrack-frequency"

    const val ACTION_PAUSE_BACKTRACK = "paths-action-pause-backtrack"
    const val ACTION_RESUME_BACKTRACK = "paths-action-resume-backtrack"

    const val SERVICE_BACKTRACK = "paths-service-backtrack"

    const val WIDGET_BACKTRACK = "paths-widget-backtrack"
}