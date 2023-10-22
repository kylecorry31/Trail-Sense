package com.kylecorry.trail_sense.astronomy.infrastructure.commands

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.NavigationUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.extensions.onDefault
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

class SunsetAlarmCommand(private val context: Context) : CoroutineCommand {

    private val location: LocationSubsystem = LocationSubsystem.getInstance(context)
    private val userPrefs: UserPreferences = UserPreferences(context)
    private val astronomyService = AstronomyService()

    private val alertWindowMinutes = 20
    private val alertWindow: Duration = Duration.ofMinutes(alertWindowMinutes)

    override suspend fun execute() = onDefault {
        Log.i(TAG, "Started")

        val currentTime = ZonedDateTime.now()

        if (location.location == Coordinate.zero) {
            setAlarm(currentTime.plusDays(1))
            return@onDefault
        }

        val alertDuration = Duration.ofMinutes(userPrefs.astronomy.sunsetAlertMinutesBefore)
        val suntimesMode = userPrefs.astronomy.sunTimesMode

        val todaySunset = astronomyService.getTodaySunTimes(location.location, suntimesMode).set

        val tomorrowSunset =
            astronomyService.getTomorrowSunTimes(location.location, suntimesMode).set

        todaySunset ?: return@onDefault

        if (isAfterSunset(todaySunset)) {
            // Missed the sunset, schedule the alarm for tomorrow
            setAlarm(
                tomorrowSunset?.minus(alertDuration)
                    ?: todaySunset.plusDays(1)
            )
            return@onDefault
        }

        if (withinAlertWindow(todaySunset, alertDuration)) {
            // Send alert, schedule alarm for tomorrow's sunset or else at some point tomorrow
            sendNotification(todaySunset)
            setAlarm(
                tomorrowSunset?.minus(alertDuration)
                    ?: todaySunset.plusDays(1)
            )
            return@onDefault
