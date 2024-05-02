package com.kylecorry.trail_sense.tools.paths

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.paths.infrastructure.services.BacktrackService
import com.kylecorry.trail_sense.tools.paths.quickactions.QuickActionBacktrack
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolDiagnostic
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

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
                        UserPreferences(it).backtrackEnabled = false
                        BacktrackScheduler.stop(it)
                    }
                )
            ),
            diagnostics = listOf(
                ToolDiagnostic.gps,
                ToolDiagnostic.altimeter,
                ToolDiagnostic.barometer,
            )
        )
    }
}