package com.kylecorry.trail_sense

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
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
        NotificationChannels.createChannels(this)
        PreferenceMigrator.getInstance().migrate(this)
        RepoCleanupWorker.scheduler(this).interval(Duration.ofHours(6))

        // Start up the weather subsystem
        WeatherSubsystem.getInstance(this)

        // Start up the flashlight subsystem
        FlashlightSubsystem.getInstance(this)
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setAvailableCamerasLimiter(CameraSelector.DEFAULT_BACK_CAMERA)
            .setMinimumLoggingLevel(Log.ERROR).build()
    }

}