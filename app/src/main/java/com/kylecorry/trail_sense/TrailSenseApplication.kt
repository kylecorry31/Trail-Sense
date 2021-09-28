package com.kylecorry.trail_sense

import android.app.Application
import android.util.Log
import com.kylecorry.andromeda.jobs.TaskSchedulerFactory
import com.kylecorry.trail_sense.settings.migrations.PreferenceMigrator
import com.kylecorry.trail_sense.shared.database.RepoCleanupWorker
import java.time.Duration

class TrailSenseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("TrailSenseApplication", "onCreate")
        NotificationChannels.createChannels(this)
        PreferenceMigrator.getInstance().migrate(this)
        val scheduler =
            TaskSchedulerFactory(this).deferrable(RepoCleanupWorker::class.java, 2739523)
        scheduler.schedule(Duration.ZERO)
    }

}