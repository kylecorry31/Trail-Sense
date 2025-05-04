package com.kylecorry.trail_sense.tools.climate.infrastructure

import com.kylecorry.sol.math.interpolation.NewtonInterpolator
import com.kylecorry.sol.time.Time.daysUntil
import java.time.LocalDate
import java.time.Month

class MonthlyValueInterpolator {

    private val newtonInterpolator = NewtonInterpolator()

    fun interpolate(date: LocalDate, monthlyValues: Map<Month, Float>): Float {
        val lookupMonths = getSurroundingMonths(date)

        val start = lookupMonths.first()
        val xs = lookupMonths.map {
            start.daysUntil(it).toFloat()
        }
        val values = lookupMonths.map {
            monthlyValues[it.month] ?: 0f
        }
        val x = start.daysUntil(date).toFloat()
        return newtonInterpolator.interpolate(x, xs, values)
    }

    private fun getSurroundingMonths(date: LocalDate): List<LocalDate> {
        val midMonth = LocalDate.of(date.year, date.month, 15)
        return if (date > midMonth) {
            listOf(
                midMonth.minusMonths(1),
                midMonth,
                // The date is between these
                midMonth.plusMonths(1),
                midMonth.plusMonths(2)
            )
        } else {
            listOf(
                midMonth.minusMonths(2),
                midMonth.minusMonths(1),
                // The date is between these
                midMonth,
                midMonth.plusMonths(1)
            )
        }
    }

}