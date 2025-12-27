package com.kylecorry.trail_sense.main

import android.content.Context
import com.kylecorry.trail_sense.main.automations.Automations
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.main.persistence.RepoCleanupWorker
import com.kylecorry.trail_sense.settings.migrations.PreferenceMigrator
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetBroadcastManager
import java.time.Duration

object TrailSenseApplicationInitializer {

    fun initialize(context: Context) {
        SafeMode.initialize(context)
        TrailSenseServiceRegister.setup(context)
        Automations.setup(context)
        WidgetBroadcastManager.setup(context)
        NotificationChannels.createChannels(context)
        PreferenceMigrator.getInstance().migrate(context)
        RepoCleanupWorker.scheduler(context).cancel()
        RepoCleanupWorker.scheduler(context).interval(Duration.ofHours(6))

        // Initialize all tools
        val tools = Tools.getTools(context)
        tools.forEach {
            it.initialize(context)
        }
    }

}