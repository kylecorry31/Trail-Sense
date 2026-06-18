package com.kylecorry.trail_sense.tools.pedometer.ui

import android.content.Context
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemMapper
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import java.time.Duration
import java.time.Instant

class PedometerSessionListItemMapper(
    private val context: Context,
    private val paceCalculator: StrideLengthPaceCalculator,
    private val onDelete: (period: StepTrackingPeriod, title: String) -> Unit
) : ListItemMapper<StepTrackingPeriod> {

    private val formatter = getAppService<FormatService>()
    private val prefs = getAppService<UserPreferences>()

    override fun map(value: StepTrackingPeriod): ListItem {
        val title = getTitle(value)
        return ListItem(
            value.id,
            title,
            subtitle = getSubtitle(value),
            menu = listOf(
                ListMenuItem(context.getString(R.string.delete)) {
                    onDelete(value, title)
                }
            )
        )
    }

    private fun getTitle(period: StepTrackingPeriod): String {
        val endTime = period.endTime ?: Instant.now()
        return formatter.formatTimeSpan(
            period.startTime.toZonedDateTime(),
            endTime.toZonedDateTime(),
            relative = true
        )
    }

    private fun getSubtitle(period: StepTrackingPeriod): String {
        val duration = Duration.between(period.startTime, period.endTime ?: Instant.now())
        val distance = paceCalculator.distance(period.steps)
            .convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
        val speed = paceCalculator.speed(period.steps, duration)
            .convertTo(DistanceUnits.Meters, TimeUnits.Seconds)
        return formatter.join(
            DecimalFormatter.format(period.steps, 0) + " " + context.getString(R.string.steps),
            formatter.formatDistance(
                distance,
                Units.getDecimalPlaces(distance.units),
                false
            ),
            if (duration.seconds > 0) {
                formatter.formatSpeed(speed.value)
            } else {
                context.getString(R.string.dash)
            },
            formatter.formatDuration(duration, short = true),
            separator = FormatService.Separator.Dot
        )
    }
}
