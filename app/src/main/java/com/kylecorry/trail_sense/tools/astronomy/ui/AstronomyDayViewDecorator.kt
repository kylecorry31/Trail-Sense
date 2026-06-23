package com.kylecorry.trail_sense.tools.astronomy.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.pickers.material.AndromedaDayViewDecorator
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.domain.Eclipse
import java.time.LocalDate
import java.time.ZoneId

class AstronomyDayViewDecorator(private val location: Coordinate) : AndromedaDayViewDecorator() {

    private var phaseImageMapper: MoonPhaseImageMapper? = null
    private var solarEclipseImageMapper: SolarEclipseImageMapper? = null

    override fun getBottomDrawable(
        context: Context,
        date: LocalDate,
        isSelected: Boolean
    ): Drawable {
        val astronomy = AstronomyService()

        val size = Resources.dp(context, 12f).toInt()
        val phase = astronomy.getMoonPhase(date)
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
        val solarEclipseDrawable =
            solarEclipse?.let { getSolarEclipseDrawable(context, astronomy, it, size) }
        val drawables = listOfNotNull(
            getPhaseImageMapper(context).getPhaseImage(
                phase.phaseAngle,
                size,
                size,
                moonTilt
            ).toDrawable(context.resources).apply { setBounds(0, 0, size, size) },
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
            solarEclipseDrawable
        )

        return if (drawables.isEmpty()) {
            createEmptyIndicatorDrawable(size * 2)
        } else {
            createIndicatorDrawableGrid(drawables, drawables.size.coerceAtMost(2), size, size / 4)
        }
    }

    private fun getSolarEclipseDrawable(
        context: Context,
        astronomy: AstronomyService,
        eclipse: Eclipse,
        size: Int
    ): Drawable {
        val sunAzimuth = astronomy.getSunAzimuth(location, eclipse.peak)
        val sunAltitude = astronomy.getSunAltitude(location, eclipse.peak)
        val moonAzimuth = astronomy.getMoonAzimuth(location, eclipse.peak)
        val moonAltitude = astronomy.getMoonAltitude(location, eclipse.peak)
        return getSolarEclipseImageMapper(context).getEclipseImage(
            CelestialObservation(sunAzimuth, sunAltitude, astronomy.getSunAngularDiameter(eclipse.peak)),
            CelestialObservation(
                moonAzimuth,
                moonAltitude,
                astronomy.getMoonAngularDiameter(location, eclipse.peak)
            ),
            eclipse.isTotal,
            size,
            size
        ).toDrawable(context.resources).apply { setBounds(0, 0, size, size) }
    }

    private fun getPhaseImageMapper(context: Context): MoonPhaseImageMapper {
        return phaseImageMapper ?: MoonPhaseImageMapper(context.applicationContext).also {
            phaseImageMapper = it
        }
    }

    private fun getSolarEclipseImageMapper(context: Context): SolarEclipseImageMapper {
        return solarEclipseImageMapper
            ?: SolarEclipseImageMapper(context.applicationContext).also {
                solarEclipseImageMapper = it
            }
    }

}
