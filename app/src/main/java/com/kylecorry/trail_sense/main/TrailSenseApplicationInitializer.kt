package com.kylecorry.trail_sense.main

import android.content.Context
import android.os.Build
import com.kylecorry.trail_sense.main.automations.Automations
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.main.persistence.RepoCleanupWorker
import com.kylecorry.trail_sense.settings.migrations.PreferenceMigrator
import com.kylecorry.trail_sense.shared.debugging.getBuildType
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetBroadcastManager
import java.time.Duration

object TrailSenseApplicationInitializer {

    fun initialize(context: Context) {
        TrailSenseServiceRegister.setup(context)
        SafeMode.initialize(context)
        getAppService<Logger>().info(TAG, "App started")

        // Initialize all tools
        val tools = Tools.getTools(context, false)
        tools.forEach {
            it.initialize(context)
        }

        Automations.setup(context)
        WidgetBroadcastManager.setup(context)
        NotificationChannels.createChannels(context)
        PreferenceMigrator.getInstance().migrate(context)
        RepoCleanupWorker.scheduler(context).cancel()
        RepoCleanupWorker.scheduler(context).interval(Duration.ofHours(6))
    }

    private const val TAG = "TrailSenseApplication"

}
