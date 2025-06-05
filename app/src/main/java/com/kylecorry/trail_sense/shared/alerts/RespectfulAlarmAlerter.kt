package com.kylecorry.trail_sense.shared.alerts

import android.content.Context
import com.kylecorry.sol.math.Range
import java.time.LocalTime

class RespectfulAlarmAlerter(context: Context, private val allowedHours: Range<LocalTime>?) :
    IAlerter {

    constructor(context: Context, isAlwaysEnabled: Boolean) : this(
        context,
        if (isAlwaysEnabled) Range(LocalTime.MIN, LocalTime.MAX) else null
    )

    private val alarm = AlarmAlerter(context)

    override fun alert() {
        if (allowedHours == null) {
            return
        }

        val now = LocalTime.now()
        if (!allowedHours.contains(now)) {
            return
        }

        alarm.alert()
    }

}