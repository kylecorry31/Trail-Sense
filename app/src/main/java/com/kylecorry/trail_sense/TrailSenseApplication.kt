package com.kylecorry.trail_sense

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.kylecorry.trail_sense.settings.migrations.PreferenceMigrator
import com.kylecorry.trail_sense.shared.database.RepoCleanupWorker
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration

class TrailSenseApplication : Application(), CameraXConfig.Provider {

    override fun onCreate() {
        super.onCreate()
        Log.d("TrailSenseApplication", "onCreate")
        createNotificationChannels()
        migratePreferences()
        scheduleRepoCleanupWorker()

        // Start up the weather subsystem
        startWeatherSubsystem()

        // Start up the flashlight subsystem
        startFlashlightSubsystem()
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.ERROR).build()
    }

    private fun createNotificationChannels() {
        NotificationChannels.createChannels(this)
    }

    private fun migratePreferences() {
        PreferenceMigrator.getInstance().migrate(this)
    }

    private fun scheduleRepoCleanupWorker() {
        RepoCleanupWorker.scheduler(this).interval(Duration.ofHours(6))
    }

    private fun startWeatherSubsystem() {
        WeatherSubsystem.getInstance(this)
    }

    private fun startFlashlightSubsystem() {
        FlashlightSubsystem.getInstance(this)
    }
}
