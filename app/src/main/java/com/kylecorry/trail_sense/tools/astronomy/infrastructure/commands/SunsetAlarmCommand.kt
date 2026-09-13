package com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.logging.Logger
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
        val now = ZonedDateTime.now()

        if (location.location == Coordinate.zero) {
            setAlarm(now.plusDays(1), "no location available")
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
                            ?: todaySunset.plusDays(1),
                        "past today's sunset at ${todaySunset.toInstant()}"
                    )
                }

                withinAlertWindow(todaySunset, alertDuration) -> {
                    // Send alert, schedule alarm for tomorrow's sunset or else at some point tomorrow
                    sendNotification(todaySunset)
                    setAlarm(
                        tomorrowSunset?.minus(alertDuration)
                            ?: todaySunset.plusDays(1),
                        "within alert window for today's sunset at ${todaySunset.toInstant()}"
                    )
                }

                else -> { // Before the alert window
                    // Schedule alarm for sunset
                    setAlarm(
                        todaySunset.minus(alertDuration),
                        "before alert window for today's sunset at ${todaySunset.toInstant()}"
                    )
                }
            }
        } else {
            // There isn't a sunset today, schedule it for tomorrow
            setAlarm(tomorrowSunset?.minus(alertDuration) ?: now.plusDays(1), "no sunset today")
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
            getAppService<Logger>().info(TAG, "Sunset alert already sent today")
            return
        }

        userPrefs.astronomy.setSunsetAlertLastSentDate(LocalDate.now())

        val formatService = FormatService.getInstance(context)
        val formattedTime = formatService.formatTime(sunset.toLocalTime(), false)

        val openIntent = NavigationUtils.pendingIntent(context, R.id.action_astronomy)

        val useAlarm = userPrefs.astronomy.useAlarmForSunsetAlert
        val notificationChannel = if (useAlarm) {
            AstronomyToolRegistration.NOTIFICATION_CHANNEL_SUNSET_ALARM
        } else {
            AstronomyToolRegistration.NOTIFICATION_CHANNEL_SUNSET_ALERT
        }
        val notification = Notify.alert(
            context,
            notificationChannel,
            context.getString(R.string.sunset_alert_notification_title),
            context.getString(
                R.string.sunset_alert_notification_text,
                formattedTime
            ),
            R.drawable.ic_sunset_notification,
            intent = openIntent,
            autoCancel = true,
            isAlarm = useAlarm,
            category = NotificationCompat.CATEGORY_REMINDER
        )

        DependencyRegistry.get<NotificationSubsystem>().send(NOTIFICATION_ID, notification)
        getAppService<Logger>().info(TAG, "Sunset alert sent")
    }

    private fun setAlarm(time: ZonedDateTime, reason: String) {
        val scheduler = SunsetAlarmReceiver.scheduler(context)
        scheduler.cancel()
        val instant = time.toInstant()
        scheduler.once(instant)
        getAppService<Logger>().info(
            TAG,
            "Scheduled next run at $instant (exact: ${Permissions.canScheduleExactAlarms(context)}): $reason"
        )
    }

    companion object {
        const val TAG = "SunsetAlarmCommand"
        const val NOTIFICATION_ID = 1231
        const val NOTIFICATION_CHANNEL_ID = "Sunset alert"
    }

}
