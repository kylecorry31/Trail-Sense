package com.kylecorry.trail_sense.main

import android.content.Context
import android.text.Layout
import android.text.style.AlignmentSpan
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.markdown.MarkdownExtension
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.device.DeviceSubsystem
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.text.HiddenSpan
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object TrailSenseServiceRegister {
    fun setup(context: Context) {
        val appContext = context.applicationContext

        // Shared services
        AppServiceRegistry.register(FormatService.getInstance(appContext))
        AppServiceRegistry.register(PreferencesSubsystem.getInstance(appContext))
        AppServiceRegistry.register(UserPreferences(appContext))
        AppServiceRegistry.register(NotificationSubsystem(appContext))
        AppServiceRegistry.register(
            MarkdownService(
                appContext, extensions = listOf(
                    MarkdownExtension(2, '%') {
                        HiddenSpan()
                    },
                    MarkdownExtension(
                        1,
                        '+'
                    ) { AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER) }
                )))
        AppServiceRegistry.register(SensorService(appContext))
        AppServiceRegistry.register(FileSubsystem.getInstance(appContext))
        AppServiceRegistry.register(AppDatabase.getInstance(appContext))
        AppServiceRegistry.register(SensorSubsystem.getInstance(appContext))
        AppServiceRegistry.register(LocationSubsystem.getInstance(appContext))
        AppServiceRegistry.register(DeviceSubsystem(appContext))

        Tools.getTools(context, false).forEach { tool ->
            tool.singletons.forEach { producer ->
                val service = producer(appContext)
                // Updating directly since it will loose the type name when using register
                AppServiceRegistry.services[service::class.java.name] = service
            }
        }

    }
}