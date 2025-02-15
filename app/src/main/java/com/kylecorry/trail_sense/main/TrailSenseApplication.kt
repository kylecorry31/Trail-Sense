package com.kylecorry.trail_sense.main

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.kylecorry.trail_sense.main.automations.Automations
import com.kylecorry.trail_sense.main.persistence.RepoCleanupWorker
import com.kylecorry.trail_sense.settings.migrations.PreferenceMigrator
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetBroadcastManager
import java.time.Duration


class TrailSenseApplication : Application(), CameraXConfig.Provider {

    override fun onCreate() {
        super.onCreate()
        Log.d("TrailSenseApplication", "onCreate")
        Automations.setup(this)
        WidgetBroadcastManager.setup(this)
        NotificationChannels.createChannels(this)
        PreferenceMigrator.getInstance().migrate(this)
        RepoCleanupWorker.scheduler(this).cancel()
        RepoCleanupWorker.scheduler(this).interval(Duration.ofHours(6))

        // Initialize all tools
        val tools = Tools.getTools(this)
        tools.forEach {
            it.initialize(this)
        }
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.ERROR).build()
    }
}