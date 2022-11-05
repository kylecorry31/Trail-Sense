package com.kylecorry.trail_sense.shared.views.chart.label

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.toRelativeDistance

class DistanceChartLabelFormatter(
    private val formatter: FormatService,
    private val fromUnits: DistanceUnits,
    private val toUnits: DistanceUnits,
    private val relative: Boolean
) : ChartLabelFormatter {
    override fun format(value: Float): String {
        val distance = Distance(value, fromUnits).convertTo(toUnits).let {
            if (relative) {
                it.toRelativeDistance()
            } else {
                it
            }
        }
        return formatter.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        )
    }
}