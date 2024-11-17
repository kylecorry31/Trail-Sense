package com.kylecorry.trail_sense.tools.flashlight

import android.content.Context
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightService
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.flashlight.quickactions.QuickActionFlashlight
import com.kylecorry.trail_sense.tools.flashlight.quickactions.QuickActionScreenFlashlight
import com.kylecorry.trail_sense.tools.flashlight.services.FlashlightToolService
import com.kylecorry.trail_sense.tools.flashlight.ui.FragmentToolScreenFlashlight
import com.kylecorry.trail_sense.tools.flashlight.volumeactions.FlashlightToggleVolumeAction
import com.kylecorry.trail_sense.tools.flashlight.volumeactions.ScreenFlashlightBrightnessVolumeAction
import com.kylecorry.trail_sense.tools.flashlight.widgets.AppWidgetFlashlight
import com.kylecorry.trail_sense.tools.flashlight.widgets.FlashlightToolWidgetView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolNotificationChannel
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolVolumeActionPriority
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object FlashlightToolRegistration : ToolRegistration {

    override fun getTool(context: Context): Tool {
        val hasFlashlight = FlashlightSubsystem.getInstance(context).isAvailable()

        return Tool(
            Tools.FLASHLIGHT,
            context.getString(R.string.flashlight_title),
            R.drawable.flashlight,
            R.id.fragmentToolFlashlight,
            ToolCategory.Signaling,
            initialize = { FlashlightSubsystem.getInstance(it) },
            guideId = R.raw.guide_tool_flashlight,
            settingsNavAction = R.id.flashlightSettingsFragment,
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
                    { context, _, _ -> UserPreferences(context).flashlight.toggleWithVolumeButtons },
                    ::FlashlightToggleVolumeAction
                ),
                ToolVolumeAction(
                    ToolVolumeActionPriority.High,
                    { context, _, fragment ->
                        fragment is FragmentToolScreenFlashlight && UserPreferences(
                            context
                        ).flashlight.controlScreenFlashlightWithVolumeButtons
                    },
                    ::ScreenFlashlightBrightnessVolumeAction
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
            services = listOf(FlashlightToolService(context)),
            widgets = listOf(
                ToolWidget(
                    WIDGET_FLASHLIGHT,
                    context.getString(R.string.flashlight_title),
                    ToolSummarySize.Half,
                    FlashlightToolWidgetView(),
                    AppWidgetFlashlight::class.java,
                    updateBroadcasts = listOf(BROADCAST_FLASHLIGHT_STATE_CHANGED),
                    isEnabled = { hasFlashlight }
                )
            ),
            broadcasts = listOf(
                ToolBroadcast(
                    BROADCAST_FLASHLIGHT_STATE_CHANGED,
                    "Flashlight state changed"
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

    const val SERVICE_FLASHLIGHT = "flashlight-service-flashlight"
    const val WIDGET_FLASHLIGHT = "flashlight-widget-flashlight"

    const val BROADCAST_FLASHLIGHT_STATE_CHANGED = "flashlight-broadcast-flashlight-state-changed"
}
