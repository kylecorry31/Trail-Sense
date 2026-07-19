package com.kylecorry.trail_sense.shared.extensions

import android.app.Notification
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.provider.Settings

@Suppress("DEPRECATION")
fun Notification.useAlarmSound(): Notification {
    category = Notification.CATEGORY_ALARM
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        sound = Settings.System.DEFAULT_NOTIFICATION_URI
        audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(AudioManager.STREAM_ALARM)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
    }
    return this
}
