package com.kylecorry.trail_sense

import android.app.Application
import android.util.Log
import com.kylecorry.trail_sense.settings.migrations.PreferenceMigrator

class TrailSenseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("TrailSenseApplication", "onCreate")
        NotificationChannels.createChannels(this)
        PreferenceMigrator.getInstance().migrate(this)
    }

}