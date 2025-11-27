package com.kylecorry.trail_sense.tools.climate.infrastructure

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.interpolation.Interpolator
import com.kylecorry.sol.math.interpolation.LocalNewtonInterpolator
import com.kylecorry.sol.time.Time.daysUntil
import java.time.LocalDate
import java.time.Month

class MonthlyValueInterpolator {

    private val interpolatorCache = mutableMapOf<Pair<Int, Map<Month, Float>>, Interpolator>()

    fun interpolate(date: LocalDate, monthlyValues: Map<Month, Float>): Float {
        val midYear = LocalDate.of(date.year, Month.JULY, 1)
        val interpolator = interpolatorCache.getOrPut(date.year to monthlyValues) {
            val dates = listOf(
                Month.JANUARY,
                Month.FEBRUARY,
                Month.MARCH,
                Month.APRIL,
                Month.MAY,
                Month.JUNE,
                Month.JULY,
                Month.AUGUST,
                Month.SEPTEMBER,
                Month.OCTOBER,
                Month.NOVEMBER,
                Month.DECEMBER
            ).map {
                LocalDate.of(midYear.year, it, 15)
            }.toMutableList()
            dates.add(0, LocalDate.of(midYear.year - 1, Month.DECEMBER, 15))
            dates.add(0, LocalDate.of(midYear.year - 1, Month.NOVEMBER, 15))
            dates.add(LocalDate.of(midYear.year + 1, Month.NOVEMBER, 15))
            dates.add(LocalDate.of(midYear.year + 1, Month.DECEMBER, 15))
            val points = dates.map {
                val value = monthlyValues[it.month] ?: 0f
                Vector2(midYear.daysUntil(it).toFloat(), value)
            }
            LocalNewtonInterpolator(points, 3)
        }

        return interpolator.interpolate(midYear.daysUntil(date).toFloat())
    }

}