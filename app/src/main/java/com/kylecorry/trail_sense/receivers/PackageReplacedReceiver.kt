package com.kylecorry.trail_sense.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.andromeda.core.system.PackageUtils
import com.kylecorry.trail_sense.shared.UserPreferences

class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED && context != null) {
            TrailSenseServiceUtils.restartServices(context)
            PackageUtils.setComponentEnabled(
                context,
                "com.kylecorry.trail_sense.AliasMainActivity",
                UserPreferences(context).navigation.areMapsEnabled
            )
        }
    }
}