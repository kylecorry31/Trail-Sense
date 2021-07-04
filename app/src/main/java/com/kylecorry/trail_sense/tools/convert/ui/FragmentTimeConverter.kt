package com.kylecorry.trail_sense.tools.convert.ui

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trailsensecore.domain.units.*

class FragmentTimeConverter : SimpleConvertFragment<TimeUnits>(TimeUnits.Minutes, TimeUnits.Hours) {

    private val formatService by lazy { FormatServiceV2(requireContext()) }

    override val units = listOf(
        TimeUnits.Seconds,
        TimeUnits.Minutes,
        TimeUnits.Hours,
        TimeUnits.Days
    )

    override fun getUnitName(unit: TimeUnits): String {
        return when (unit) {
            TimeUnits.Seconds -> getString(R.string.seconds)
            TimeUnits.Minutes -> getString(R.string.minutes)
            TimeUnits.Hours -> getString(R.string.hours)
            TimeUnits.Days -> getString(R.string.days)
            else -> ""
        }
    }

    override fun convert(amount: Float, from: TimeUnits, to: TimeUnits): String {
        val seconds = amount * from.seconds
        val converted = seconds / to.seconds
        return formatService.formatTime(converted, to, 4, false)
    }

}