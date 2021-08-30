package com.kylecorry.trail_sense.astronomy.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.LocationCommand

class TestAlertCommand(private val context: Context) : LocationCommand {
    override fun execute(location: Coordinate) {
        val prefs = UserPreferences(context)
        val shouldSend = prefs.astronomy.sendAstronomyAlerts

        if (!shouldSend) {
            return
        }

        val notification = Notify.status(
            context,
            NotificationChannels.CHANNEL_ASTRONOMY_ALERTS,
            "Test alert",
            "This is a test of the astronomy alert system",
            R.drawable.ic_astronomy,
            group = NotificationChannels.GROUP_ASTRONOMY_ALERTS,
            intent = NavigationUtils.pendingIntent(context, R.id.action_astronomy)
        )

        Notify.send(context, 6968776, notification)
    }

}