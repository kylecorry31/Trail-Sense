package com.kylecorry.trail_sense.tools.paths

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.paths.actions.PauseBacktrackAction
import com.kylecorry.trail_sense.tools.paths.actions.ResumeBacktrackAction
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.paths.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.tools.paths.infrastructure.subsystem.BacktrackSubsystem
import com.kylecorry.trail_sense.tools.paths.quickactions.QuickActionBacktrack
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
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
            notificationChannels = listOf(
                ToolNotificationChannel(
                    BacktrackService.FOREGROUND_CHANNEL_ID,
                    context.getString(R.string.backtrack),
                    context.getString(R.string.backtrack_notification_channel_description),
                    Notify.CHANNEL_IMPORTANCE_LOW,
                    muteSound = true
                )
            ),
            services = listOf(
                ToolService(
                    context.getString(R.string.backtrack),
                    getFrequency = { UserPreferences(it).backtrackRecordFrequency },
                    isActive = {
                        BacktrackScheduler.isOn(it)
                    },
                    disable = {
                        BacktrackSubsystem.getInstance(it).disable()
                    },
                    stop = {
                        BacktrackScheduler.stop(it)
                    },
                    restart = {
                        val backtrack = BacktrackSubsystem.getInstance(context)
                        if (backtrack.getState() == FeatureState.On) {
                            if (!BacktrackService.isRunning) {
                                BacktrackScheduler.start(it, false)
                            }
                        } else {
                            BacktrackScheduler.stop(it)
                        }
                    }
                )
            ),
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

    const val ACTION_PAUSE_BACKTRACK = "paths-action-pause-backtrack"
    const val ACTION_RESUME_BACKTRACK = "paths-action-resume-backtrack"
}