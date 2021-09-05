package com.kylecorry.trail_sense.astronomy.infrastructure.commands

import android.content.Context
import android.util.Log
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.sol.science.astronomy.meteors.MeteorShowerPeak
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.LocationCommand
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class MeteorShowerAlertCommand(private val context: Context) : LocationCommand {
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

        val notification = Notify.status(
            context,
            NotificationChannels.CHANNEL_ASTRONOMY_ALERTS,
            context.getString(R.string.meteor_shower),
            getShowerDescription(context, shower),
            R.drawable.ic_astronomy,
            group = NotificationChannels.GROUP_ASTRONOMY_ALERTS,
            intent = NavigationUtils.pendingIntent(context, R.id.action_astronomy)
        )

        Notify.send(context, 732094, notification)
    }

    private fun getShowerDescription(context: Context, shower: MeteorShowerPeak): String {
        val formatService = FormatService(context)

        val peak =
            formatService.formatRelativeDate(shower.peak.toLocalDate()) + " " + formatService.formatTime(
                shower.peak.toLocalTime(),
                false
            )

        val rate = context.getString(R.string.meteors_per_hour, shower.shower.rate)

        return "$peak\n$rate"
    }

    companion object {
        private const val TAG = "MeteorShowerAlertCommand"
    }

}