package com.kylecorry.trail_sense.shared

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.kylecorry.andromeda.core.system.Intents

fun Intents.notificationSettings(context: Context, channel: String? = null): Intent {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return appSettings(context)
    }

    return if (channel == null) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channel)
        }
    }
}