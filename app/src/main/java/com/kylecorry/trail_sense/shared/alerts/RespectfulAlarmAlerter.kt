package com.kylecorry.trail_sense.shared.alerts

import android.content.Context
import com.kylecorry.sol.math.Range
import java.time.LocalTime

class RespectfulAlarmAlerter(
    context: Context,
    private val allowedHours: Range<LocalTime>?,
    notificationChannel: String
) :
    IAlerter {

    constructor(context: Context, isAlwaysEnabled: Boolean, notificationChannel: String) : this(
        context,
        if (isAlwaysEnabled) Range(LocalTime.MIN, LocalTime.MAX) else null,
        notificationChannel
    )

    private val alarm = AlarmAlerter(context, notificationChannel)

    override fun alert() {
        if (allowedHours == null) {
            return
        }

        val now = LocalTime.now()

        if (allowedHours.start <= allowedHours.end && !allowedHours.contains(now)) {
            // Notifications during the day, but it's not time yet
            return
        } else if (allowedHours.start > allowedHours.end && Range(
                allowedHours.end,
                allowedHours.start
            ).contains(now)
        ) {
            // The user selected to have notification only at night, so we need to invert the range check
            return
        }
        alarm.alert()
    }

}