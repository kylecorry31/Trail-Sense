package com.kylecorry.trail_sense.tools.whitenoise

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.volume.SystemVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import com.kylecorry.trail_sense.tools.whitenoise.quickactions.QuickActionWhiteNoise
import com.kylecorry.trail_sense.tools.whitenoise.services.WhiteNoiseToolService

object WhiteNoiseToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.WHITE_NOISE,
            context.getString(R.string.tool_white_noise_title),
            R.drawable.ic_tool_white_noise,
            R.id.fragmentToolWhiteNoise,
            ToolCategory.Other,
            context.getString(R.string.tool_white_noise_summary),
            guideId = R.raw.guide_tool_white_noise,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_WHITE_NOISE,
                    context.getString(R.string.tool_white_noise_title),
                    ::QuickActionWhiteNoise
                )
            ),
            volumeActions = listOf(
                ToolVolumeAction(
                    ToolVolumeActionPriority.High,
                    { _, isToolOpen, _ -> isToolOpen || WhiteNoiseService.isRunning },
                    ::SystemVolumeAction
                )
            ),
            notificationChannels = listOf(
                ToolNotificationChannel(
                    WhiteNoiseService.NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.tool_white_noise_title),
                    context.getString(R.string.tool_white_noise_title),
                    Notify.CHANNEL_IMPORTANCE_LOW
                )
            ),
            services = listOf(WhiteNoiseToolService(context)),
            diagnostics = listOf(
                ToolDiagnosticFactory.notification(
                    WhiteNoiseService.NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.tool_white_noise_title)
                )
            )
        )
    }

    const val SERVICE_WHITE_NOISE = "whitenoise-service-white-noise"
}