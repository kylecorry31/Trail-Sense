package com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.AlarmAlerter
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

class SunsetAlarmCommand(private val context: Context) : CoroutineCommand {

    private val location by lazy { LocationSubsystem.getInstance(context) }
    private val userPrefs by lazy { UserPreferences(context) }
    private val astronomyService = AstronomyService()

    // The window prior the alert time that the alert can be sent
    private val alertWindow = Duration.ofMinutes(20)

    override suspend fun execute() = onDefault {
        Log.i(TAG, "Started")

        val now = ZonedDateTime.now()

        if (location.location == Coordinate.zero) {
            setAlarm(now.plusDays(1))
            return@onDefault
        }

        val alertDuration = Duration.ofMinutes(userPrefs.astronomy.sunsetAlertMinutesBefore)
        val suntimesMode = userPrefs.astronomy.sunTimesMode

        val todaySunset = astronomyService.getTodaySunTimes(location.location, suntimesMode).set

        val tomorrowSunset =
            astronomyService.getTomorrowSunTimes(location.location, suntimesMode).set

        if (todaySunset != null) {
            when {
                isPastSunset(todaySunset) -> {
                    // Missed the sunset, schedule the alarm for tomorrow
                    setAlarm(
                        tomorrowSunset?.minus(alertDuration)
                            ?: todaySunset.plusDays(1)
                    )
                }

                withinAlertWindow(todaySunset, alertDuration) -> {
                    // Send alert, schedule alarm for tomorrow's sunset or else at some point tomorrow
                    sendNotification(todaySunset)
                    setAlarm(
                        tomorrowSunset?.minus(alertDuration)
                            ?: todaySunset.plusDays(1)
                    )
                }

                else -> { // Before the alert window
                    // Schedule alarm for sunset
                    setAlarm(todaySunset.minus(alertDuration))
                }
            }
        } else {
            // There isn't a sunset today, schedule it for tomorrow
            setAlarm(tomorrowSunset?.minus(alertDuration) ?: now.plusDays(1))
        }
    }

    private fun isPastSunset(sunset: ZonedDateTime): Boolean {
        return ZonedDateTime.now().isAfter(sunset)
    }

    private fun withinAlertWindow(sunset: ZonedDateTime, alertDuration: Duration): Boolean {
        val alertTime = sunset.minus(alertDuration)
        val minAlertTime = alertTime.minus(alertWindow)
        val alertRange = Range(minAlertTime, sunset)
        return alertRange.contains(ZonedDateTime.now())
    }

    private fun sendNotification(sunset: ZonedDateTime) {

        val lastSentDate = userPrefs.astronomy.sunsetAlertLastSent
        if (LocalDate.now() == lastSentDate) {
            return
        }

        userPrefs.astronomy.setSunsetAlertLastSentDate(LocalDate.now())

        val formatService = FormatService.getInstance(context)
        val formattedTime = formatService.formatTime(sunset.toLocalTime(), false)

        val openIntent = NavigationUtils.pendingIntent(context, R.id.action_astronomy)

        val useAlarm = userPrefs.astronomy.useAlarmForSunsetAlert
        val notification = Notify.alert(
            context,
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.sunset_alert_notification_title),
            context.getString(
                R.string.sunset_alert_notification_text,
                formattedTime
            ),
            R.drawable.ic_sunset_notification,
            intent = openIntent,
            autoCancel = true,
            mute = useAlarm
        )

        AppServiceRegistry.get<NotificationSubsystem>().send(NOTIFICATION_ID, notification)

        val alarm = AlarmAlerter(
            context,
            useAlarm,
            AstronomyToolRegistration.NOTIFICATION_CHANNEL_SUNSET_ALERT
        )
        alarm.alert()
    }

    private fun setAlarm(time: ZonedDateTime) {
        val scheduler = SunsetAlarmReceiver.scheduler(context)
        scheduler.cancel()
        scheduler.once(time.toInstant())
        Log.i(TAG, "Scheduled next run at $time")
    }

    companion object {
        const val TAG = "SunsetAlarmCommand"
        const val NOTIFICATION_ID = 1231
        const val NOTIFICATION_CHANNEL_ID = "Sunset alert"
    }

}