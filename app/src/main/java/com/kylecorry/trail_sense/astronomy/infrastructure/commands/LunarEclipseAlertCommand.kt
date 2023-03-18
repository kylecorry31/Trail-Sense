package com.kylecorry.trail_sense.astronomy.infrastructure.commands

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.NotificationChannels
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.domain.Eclipse
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.generic.Command
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class LunarEclipseAlertCommand(private val context: Context) : Command<Coordinate> {
    override fun execute(location: Coordinate) {
        val prefs = UserPreferences(context)
        val shouldSend = prefs.astronomy.sendLunarEclipseAlerts

        if (!shouldSend) {
            return
        }

        val astronomyService = AstronomyService()
        val today = LocalDate.now()
        val todayEclipse = astronomyService.getLunarEclipse(location, today)
        val tomorrowEclipse =
            astronomyService.getLunarEclipse(location, today.plusDays(1))

        val eclipse = listOfNotNull(todayEclipse, tomorrowEclipse).firstOrNull {
            val timeUntilPeak = Duration.between(LocalDateTime.now(), it.peak)
            timeUntilPeak >= Duration.ZERO && timeUntilPeak <= Duration.ofDays(1)
        }

        if (eclipse == null) {
            Log.d(TAG, "No lunar eclipse found for $today")
            return
        }

        val notification = Notify.status(
            context,
            NotificationChannels.CHANNEL_ASTRONOMY_ALERTS,
            context.getString(R.string.lunar_eclipse),
            getEclipseDescription(context, eclipse),
            R.drawable.ic_astronomy,
            group = NotificationChannels.GROUP_ASTRONOMY_ALERTS,
            intent = NavigationUtils.pendingIntent(context, R.id.action_astronomy),
            autoCancel = true
        )

        Notify.send(context, 7394232, notification)
    }

    private fun getEclipseDescription(context: Context, eclipse: Eclipse): String {
        val formatService = FormatService.getInstance(context)

        val timeSpan = formatService.formatTimeSpan(
            eclipse.start,
            eclipse.end,
            true
        )

        val magnitude = if (eclipse.isTotal) {
            context.getString(R.string.total)
        } else {
            context.getString(
                R.string.partial,
                formatService.formatPercentage(eclipse.magnitude * 100)
            )
        }

        return "$timeSpan\n$magnitude"
    }

    companion object {
        private const val TAG = "LunarEclipseAlertCommand"
    }

}