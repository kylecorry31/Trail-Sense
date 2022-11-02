package com.kylecorry.trail_sense.shared.views.chart.label

import android.content.Context
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Instant
import java.time.LocalTime

class HourChartLabelFormatter(
    context: Context,
    private val getStartTime: () -> Instant
) : ChartLabelFormatter {

    private val formatter = FormatService(context)

    override fun format(value: Float): String {
        val duration = Time.hours(value.toDouble())
        val time = getStartTime().plus(duration)
        val local = time.toZonedDateTime().toLocalTime()
        val hour = if (local.minute >= 30) {
            local.hour + 1
        } else {
            local.hour
        }
        return formatter.formatTime(
            LocalTime.of(hour % 24, 0),
            includeSeconds = false,
            includeMinutes = false
        )
    }

}