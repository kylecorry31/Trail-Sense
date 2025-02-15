package com.kylecorry.trail_sense.main

import android.content.Context
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator

object TrailSenseServiceRegister {
    fun setup(context: Context) {
        val appContext = context.applicationContext

        // Shared services
        AppServiceRegistry.register(FormatService.getInstance(appContext))
        AppServiceRegistry.register(UserPreferences(appContext))
        AppServiceRegistry.register(MarkdownService(appContext))
        AppServiceRegistry.register(SensorService(appContext))

        // Tool services (TODO: Make this part of the tool registration process)
        AppServiceRegistry.register(Navigator.getInstance(appContext))
    }
}