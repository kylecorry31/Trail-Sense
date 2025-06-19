package com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.sol.science.astronomy.meteors.MeteorShowerPeak
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
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

        val notification = Notify.status(
            context,
            AstronomyAlertCommand.NOTIFICATION_CHANNEL,
            context.getString(R.string.meteor_shower),
            getShowerDescription(context, shower),
            R.drawable.ic_astronomy,
            group = AstronomyAlertCommand.NOTIFICATION_GROUP_ASTRONOMY_ALERTS,
            intent = NavigationUtils.pendingIntent(context, R.id.action_astronomy),
            autoCancel = true
        )

        AppServiceRegistry.get<NotificationSubsystem>().send(732094, notification)
    }

    private fun getShowerDescription(context: Context, shower: MeteorShowerPeak): String {
        val formatService = FormatService.getInstance(context)

        val peak = formatService.formatRelativeDateTime(shower.peak, includeSeconds = false)

        val rate = context.getString(R.string.meteors_per_hour, shower.shower.rate)

        return "$peak\n$rate"
    }

    companion object {
        private const val TAG = "MeteorShowerAlertCommand"
    }

}