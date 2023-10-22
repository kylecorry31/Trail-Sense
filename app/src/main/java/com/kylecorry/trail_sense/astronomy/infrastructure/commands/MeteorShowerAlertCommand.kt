package com.kylecorry.trail_sense.astronomy.infrastructure.commands

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.sol.science.astronomy.meteors.MeteorShowerPeak
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.generic.Command
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class MeteorShowerAlertCommand(private val context: Context) : Command<Coordinate> {
    override fun execute(location: Coordinate) {
        val prefs = UserPreferences(context)
        val shouldSend = prefs.astronomy.sendMeteorShowerAlerts

        if (!shouldSend) {
            return
        }

        val astronomyService = AstronomyService()
        val today = LocalDate.now()
        val todayShower = astronomyService.getMeteorShower(location, today)
        val tomorrowShower = astronomyService.getMeteorShower(location, today.plusDays(1))

        val shower = listOfNotNull(todayShower, tomorrowShower).firstOrNull {
            val timeUntilPeak = Duration.between(LocalDateTime.now(), it.peak)
            timeUntilPeak >= Duration.ZERO && timeUntilPeak <= Duration.ofDays(1)
        }

        if (shower == null) {
            Log.d(TAG, "No meteor shower found for $today")
            return
        }

        val notification = createNotification(shower)

        Notify.send(context, NOTIFICATION_ID, notification)
    }

    private fun createNotification(shower: MeteorShowerPeak): Notify.StatusNotification {
        val notificationTitle = context.getString(R.string.meteor_shower)
        val notificationDescription = getShowerDescription(shower)
        val notificationIcon = R.drawable.ic_astronomy
        val notificationGroup = NotificationChannels.GROUP_ASTRONOMY_ALERTS
        val notificationIntent = NavigationUtils.pendingIntent(context, R.id.action_astronomy)
        val autoCancel = true

        return Notify.status(
            context,
            NotificationChannels.CHANNEL_ASTRONOMY_ALERTS,
            notificationTitle,
            notificationDescription,
            notificationIcon,
            group = notificationGroup,
            intent = notificationIntent,
            autoCancel = autoCancel
        )
    }

    private fun getShowerDescription(shower: MeteorShowerPeak): String {
        val formatService = FormatService.getInstance(context)

        val peak = formatService.formatRelativeDateTime(shower.peak)
        val rate = context.getString(R.string.meteors_per_hour, shower.shower.rate)

        return "$peak\n$rate"
    }

    companion object {
        private const val TAG = "MeteorShowerAlertCommand"
        private const val NOTIFICATION_ID = 732094
    }
}
