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
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunriseAlarmReceiver
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

class SunriseAlarmCommand(private val context: Context) : CoroutineCommand {

    private val location by lazy { LocationSubsystem.getInstance(context) }
    private val userPrefs by lazy { UserPreferences(context) }
    private val astronomyService = AstronomyService()

    // The window prior the alert time that the alert can be sent
    private val alertWindow = Duration.ofMinutes(20)

    // The window after the alert time that the alert can be sent
    private val alertWindowAfter = Duration.ofMinutes(5)

    override suspend fun execute() = onDefault {
        Log.i(TAG, "Started")

        val now = ZonedDateTime.now()

        if (location.location == Coordinate.zero) {
            setAlarm(now.plusDays(1))
            return@onDefault
        }

        val alertDuration = Duration.ofMinutes(userPrefs.astronomy.sunriseAlertMinutesBefore)
        val suntimesMode = userPrefs.astronomy.sunTimesMode

        val todaySunrise = astronomyService.getTodaySunTimes(location.location, suntimesMode).rise

        val tomorrowSunrise =
            astronomyService.getTomorrowSunTimes(location.location, suntimesMode).rise

        if (todaySunrise != null) {
            when {
                isPastSunrise(todaySunrise) -> {
                    // Missed the sunrise, schedule the alarm for tomorrow
                    setAlarm(
                        tomorrowSunrise?.minus(alertDuration)
                            ?: todaySunrise.plusDays(1)
                    )
                }

                withinAlertWindow(todaySunrise, alertDuration) -> {
                    // Send alert, schedule alarm for tomorrow's sunrise or else at some point tomorrow
                    sendNotification(todaySunrise)
                    setAlarm(
                        tomorrowSunrise?.minus(alertDuration)
                            ?: todaySunrise.plusDays(1)
                    )
                }

                else -> { // Before the alert window
                    // Schedule alarm for sunrise
                    setAlarm(todaySunrise.minus(alertDuration))
                }
            }
        } else {
            // There isn't a sunrise today, schedule it for tomorrow
            setAlarm(tomorrowSunrise?.minus(alertDuration) ?: now.plusDays(1))
        }
    }

    private fun isPastSunrise(sunrise: ZonedDateTime): Boolean {
        return ZonedDateTime.now().isAfter(sunrise.plus(alertWindowAfter))
    }

    private fun withinAlertWindow(sunrise: ZonedDateTime, alertDuration: Duration): Boolean {
        val alertTime = sunrise.minus(alertDuration)
        val minAlertTime = alertTime.minus(alertWindow)
        val alertRange = Range(minAlertTime, sunrise.plus(alertWindowAfter))
        return alertRange.contains(ZonedDateTime.now())
    }

    private fun sendNotification(sunrise: ZonedDateTime) {

        val lastSentDate = userPrefs.astronomy.sunriseAlertLastSent
        if (LocalDate.now() == lastSentDate) {
            return
        }

        userPrefs.astronomy.setSunriseAlertLastSentDate(LocalDate.now())

        val formatService = FormatService.getInstance(context)
        val formattedTime = formatService.formatTime(sunrise.toLocalTime(), false)

        val openIntent = NavigationUtils.pendingIntent(context, R.id.action_astronomy)

        val useAlarm = userPrefs.astronomy.useAlarmForSunriseAlert
        val notification = Notify.alert(
            context,
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.sunrise_alert_notification_title),
            context.getString(
                R.string.sunrise_alert_notification_text,
                formattedTime
            ),
            R.drawable.ic_sunrise_notification,
            intent = openIntent,
            autoCancel = true,
            mute = useAlarm
        )

        AppServiceRegistry.get<NotificationSubsystem>().send(NOTIFICATION_ID, notification)

        val alarm = AlarmAlerter(
            context,
            useAlarm,
            AstronomyToolRegistration.NOTIFICATION_CHANNEL_SUNRISE_ALERT
        )
        alarm.alert()
    }

    private fun setAlarm(time: ZonedDateTime) {
        val scheduler = SunriseAlarmReceiver.scheduler(context)
        scheduler.cancel()
        scheduler.once(time.toInstant())
        Log.i(TAG, "Scheduled next run at $time")
    }

    companion object {
        const val TAG = "SunriseAlarmCommand"
        const val NOTIFICATION_ID = 1232
        const val NOTIFICATION_CHANNEL_ID = "Sunrise alert"
    }

}