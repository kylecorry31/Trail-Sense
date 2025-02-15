package com.kylecorry.trail_sense.tools.astronomy.ui

import android.content.Context
import android.graphics.drawable.Drawable
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.pickers.material.AndromedaDayViewDecorator
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import java.time.LocalDate
import java.time.ZoneId

class AstronomyDayViewDecorator(private val location: Coordinate) : AndromedaDayViewDecorator() {

    private val phaseImageMapper = MoonPhaseImageMapper()

    override fun getBottomDrawable(
        context: Context,
        date: LocalDate,
        isSelected: Boolean
    ): Drawable {
        val astronomy = AstronomyService()

        val size = Resources.dp(context, 12f).toInt()
        val phase = astronomy.getMoonPhase(date).phase
        val moonTilt =
            astronomy.getMoonTilt(
                location,
                date.atTime(12, 0).atZone(ZoneId.systemDefault()),
                useNearestTransit = true
            )
        val hasMeteorShower = astronomy.getMeteorShower(location, date) != null
        val lunarEclipse = astronomy.getLunarEclipse(location, date)
        val hasPartialLunar = lunarEclipse != null && !lunarEclipse.isTotal
        val hasTotalLunar = lunarEclipse?.isTotal == true
        val solarEclipse = astronomy.getSolarEclipse(location, date)
        val hasPartialSolar = solarEclipse != null && !solarEclipse.isTotal
        val hasTotalSolar = solarEclipse?.isTotal == true
        val drawables = listOfNotNull(
            createIndicatorDrawable(
                context,
                phaseImageMapper.getPhaseImage(phase),
                size,
                rotation = moonTilt
            ),
            if (hasMeteorShower) createIndicatorDrawable(
                context,
                R.drawable.ic_meteor,
                size,
                Resources.androidTextColorSecondary(context)
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