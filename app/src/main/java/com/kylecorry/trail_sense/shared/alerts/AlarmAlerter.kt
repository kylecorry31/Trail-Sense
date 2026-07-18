package com.kylecorry.trail_sense.shared.alerts

import android.content.Context
import android.os.Build
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.shared.permissions.canPlayAlarmInBackground

class AlarmAlerter(
    private val context: Context,
    private val isEnabled: Boolean,
    private val notificationChannel: String,
    private val isInForeground: Boolean = false
) :
    IAlerter {

    override fun alert() {
        val hasPermission = isInForeground || Permissions.canPlayAlarmInBackground(context)
        if (!isEnabled || !hasPermission) {
            return
        }

        if (!isInForeground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            AlarmService.start(context, notificationChannel)
        } else {
            AlarmPlayer(context, notificationChannel).play()
        }
    }
}
