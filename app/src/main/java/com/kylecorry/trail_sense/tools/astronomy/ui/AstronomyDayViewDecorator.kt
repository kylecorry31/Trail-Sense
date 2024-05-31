package com.kylecorry.trail_sense.tools.astronomy.ui

import android.content.Context
import android.graphics.drawable.Drawable
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.andromeda_temporary.AndromedaDayViewDecorator
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import java.time.LocalDate

class AstronomyDayViewDecorator(private val location: Coordinate) : AndromedaDayViewDecorator() {

    private val moonIconMap = mapOf(
        MoonTruePhase.Full to R.drawable.ic_moon,
        MoonTruePhase.New to R.drawable.ic_moon_new,
        MoonTruePhase.FirstQuarter to R.drawable.ic_moon_first_quarter,
        MoonTruePhase.ThirdQuarter to R.drawable.ic_moon_third_quarter,
        MoonTruePhase.WaningCrescent to R.drawable.ic_moon_waning_crescent,
        MoonTruePhase.WaningGibbous to R.drawable.ic_moon_waning_gibbous,
        MoonTruePhase.WaxingCrescent to R.drawable.ic_moon_waxing_crescent,
        MoonTruePhase.WaxingGibbous to R.drawable.ic_moon_waxing_gibbous,
    )

    override fun getBottomDrawable(
        context: Context,
        date: LocalDate,
        isSelected: Boolean
    ): Drawable {
        val astronomy = AstronomyService()

        val size = Resources.dp(context, 12f).toInt()
        val phase = astronomy.getMoonPhase(date).phase
        val hasMeteorShower = astronomy.getMeteorShower(location, date) != null
        val lunarEclipse = astronomy.getLunarEclipse(location, date)
        val hasPartialLunar = lunarEclipse != null && !lunarEclipse.isTotal
        val hasTotalLunar = lunarEclipse?.isTotal ?: false
        val solarEclipse = astronomy.getSolarEclipse(location, date)
        val hasPartialSolar = solarEclipse != null && !solarEclipse.isTotal
        val hasTotalSolar = solarEclipse?.isTotal ?: false
        val drawables = listOfNotNull(
            moonIconMap[phase]?.let { createIndicatorDrawable(context, it, size) },
            if (hasMeteorShower) createIndicatorDrawable(
                context,
                R.drawable.ic_meteor,
                size
            ) else null,
            if (hasTotalLunar) createIndicatorDrawable(
                context,
                R.drawable.ic_moon_total_eclipse,
                size
            ) else null,
            if (hasPartialLunar) createIndicatorDrawable(
                context,
                R.drawable.ic_moon_partial_eclipse,
                size
            ) else null,
            if (hasPartialSolar) createIndicatorDrawable(
                context,
                R.drawable.ic_partial_solar_eclipse,
                size
            ) else null,
            if (hasTotalSolar) createIndicatorDrawable(
                context,
                R.drawable.ic_total_solar_eclipse,
                size
            ) else null
        )

        return if (drawables.isEmpty()) {
            createEmptyIndicatorDrawable(size * 2)
        } else {
            createIndicatorDrawableGrid(drawables, drawables.size.coerceAtMost(2), size, size / 4)
        }
    }

}