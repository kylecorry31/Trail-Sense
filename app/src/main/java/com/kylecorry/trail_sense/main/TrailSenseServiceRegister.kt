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
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.text.HiddenSpan
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator

object TrailSenseServiceRegister {
    fun setup(context: Context) {
        val appContext = context.applicationContext

        // Shared services
        AppServiceRegistry.register(FormatService.getInstance(appContext))
        AppServiceRegistry.register(PreferencesSubsystem.getInstance(appContext))
        AppServiceRegistry.register(UserPreferences(appContext))
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

        // Tool services (TODO: Make this part of the tool registration process)
        AppServiceRegistry.register(Navigator.getInstance(appContext))
        AppServiceRegistry.register(FieldGuideRepo.getInstance(appContext))
    }
}