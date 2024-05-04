package com.kylecorry.trail_sense.tools.flashlight

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightService
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.flashlight.quickactions.QuickActionFlashlight
import com.kylecorry.trail_sense.tools.flashlight.quickactions.QuickActionScreenFlashlight
import com.kylecorry.trail_sense.tools.flashlight.volumeactions.FlashlightToggleVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

object FlashlightToolRegistration : ToolRegistration {

    override fun getTool(context: Context): Tool {
        val hasFlashlight = FlashlightSubsystem.getInstance(context).isAvailable()

        return Tool(
            Tools.FLASHLIGHT,
            context.getString(R.string.flashlight_title),
            R.drawable.flashlight,
            R.id.fragmentToolFlashlight,
            ToolCategory.Signaling,
            guideId = R.raw.guide_tool_flashlight,
            // The only settings available are for the physical flashlight
            settingsNavAction = if (hasFlashlight) R.id.flashlightSettingsFragment else null,
            quickActions = listOfNotNull(
                if (hasFlashlight)
                    ToolQuickAction(
                        Tools.QUICK_ACTION_FLASHLIGHT,
                        context.getString(R.string.flashlight_title),
                        ::QuickActionFlashlight
                    ) else null,
                ToolQuickAction(
                    Tools.QUICK_ACTION_SCREEN_FLASHLIGHT,
                    context.getString(R.string.screen_flashlight_full_name),
                    ::QuickActionScreenFlashlight
                )
            ),
            additionalNavigationIds = listOf(
                R.id.fragmentToolScreenFlashlight
            ),
            volumeActions = listOf(
                ToolVolumeAction(
                    ToolVolumeActionPriority.Normal,
                    { context, _ -> UserPreferences(context).flashlight.toggleWithVolumeButtons },
                    ::FlashlightToggleVolumeAction
                )
            ),
            notificationChannels = listOf(
                ToolNotificationChannel(
                    FlashlightService.CHANNEL_ID,
                    context.getString(R.string.flashlight_title),
                    context.getString(R.string.flashlight_title),
                    Notify.CHANNEL_IMPORTANCE_LOW,
                    muteSound = true
                )
            ),
            services = listOf(
                ToolService(
                    context.getString(R.string.flashlight_title),
                    getFrequency = { Duration.ZERO },
                    isActive = {
                        FlashlightSubsystem.getInstance(it).getMode() != FlashlightMode.Off
                    },
                    disable = {
                        FlashlightSubsystem.getInstance(it).set(FlashlightMode.Off)
                    }
                )
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.flashlight(context),
                ToolDiagnosticFactory.notification(
                    FlashlightService.CHANNEL_ID,
                    context.getString(R.string.flashlight_title),
                )
            )
        )
    }
}