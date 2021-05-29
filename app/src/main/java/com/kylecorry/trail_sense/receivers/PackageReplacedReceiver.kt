package com.kylecorry.trail_sense.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trailsensecore.infrastructure.sensors.SensorChecker
import com.kylecorry.trailsensecore.infrastructure.system.PackageUtils
import com.kylecorry.trailsensecore.infrastructure.system.tryOrNothing

class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED && context != null) {
            TrailSenseServiceUtils.restartServices(context)
            PackageUtils.setComponentEnabled(
                context,
                "com.kylecorry.trail_sense.AliasMainActivity",
                UserPreferences(context).experimentalEnabled
            )
            tryOrNothing {
                PackageUtils.setComponentEnabled(
                    context,
                    "com.kylecorry.trail_sense.tiles.WeatherMonitorTile",
                    SensorChecker(context).hasBarometer()
                )
            }
        }
    }
}