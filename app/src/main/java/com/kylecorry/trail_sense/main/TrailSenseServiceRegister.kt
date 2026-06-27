package com.kylecorry.trail_sense.main

import android.content.Context
import android.text.Layout
import android.text.style.AlignmentSpan
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.markdown.MarkdownExtension
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.device.DeviceSubsystem
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.map_layers.MapLayerLoader
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance.PersistentTileCache
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.text.HiddenSpan
import com.kylecorry.trail_sense.shared.text.StringLoader
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object TrailSenseServiceRegister {
    fun setup(context: Context) {
        val appContext = context.applicationContext

        // Shared services
        DependencyRegistry.addSingleton(appContext)
        DependencyRegistry.addSingleton(StringLoader(appContext))
        DependencyRegistry.addSingleton(FormatService.getInstance(appContext))
        DependencyRegistry.addSingleton(PreferencesSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton(UserPreferences(appContext))
        DependencyRegistry.addSingleton(NotificationSubsystem(appContext))
        DependencyRegistry.addSingleton(
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
        DependencyRegistry.addSingleton(SensorService(appContext))
        DependencyRegistry.addSingleton(FileSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton(AppDatabase.getInstance(appContext))
        DependencyRegistry.addSingleton(SensorSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton(LocationSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton(DeviceSubsystem(appContext))
        DependencyRegistry.addSingleton(PluginSubsystem.getInstance(appContext))

        // Map layers
        DependencyRegistry.addSingleton(MapLayerLoader(appContext))
        DependencyRegistry.addSingleton(MapLayerPreferenceRepo())
        DependencyRegistry.addSingleton(PersistentTileCache(appContext))
    }
}

inline fun <reified T : Any> getAppService(): T {
    return DependencyRegistry.get()
}
