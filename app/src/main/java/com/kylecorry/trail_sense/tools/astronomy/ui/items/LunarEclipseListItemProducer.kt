package com.kylecorry.trail_sense.tools.astronomy.ui.items

import android.content.Context
import androidx.core.graphics.drawable.toDrawable
import com.kylecorry.andromeda.views.list.DrawableListIcon
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.science.astronomy.eclipse.LunarEclipseShadow
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.MoonTilt
import com.kylecorry.trail_sense.tools.astronomy.ui.LunarEclipseImageMapper
import com.kylecorry.trail_sense.tools.astronomy.ui.format.EclipseFormatter
import java.time.LocalDate

class LunarEclipseListItemProducer(context: Context) : BaseAstroListItemProducer(context) {

    override suspend fun getListItem(
        date: LocalDate,
        location: Coordinate,
        declination: Float
    ): ListItem? = onDefault {
        val eclipse = astronomyService.getLunarEclipse(location, date) ?: return@onDefault null

        // Advanced
        val moon = astronomyService.getMoonPosition(location, eclipse.peak)
        val peakAzimuth = DeclinationUtils.fromTrueNorthBearing(
            moon.azimuth,
            declination
        )
        val shadow = astronomyService.getLunarEclipseShadow(location, eclipse.peak)
        val moonTilt = astronomyService.getMoonTilt(location, eclipse.peak)

        val times = times(eclipse.start, eclipse.peak, eclipse.end, date)

        list(
            4,
            context.getString(R.string.lunar_eclipse),
            EclipseFormatter.type(context, eclipse),
            getIcon(moon, shadow, moonTilt),
            data = times
        ) {
            val advancedData = listOf(
                context.getString(R.string.times) to times,
                context.getString(R.string.duration) to duration(eclipse.duration),
                context.getString(R.string.obscuration) to data(
                    EclipseFormatter.type(context, eclipse)
                ),
                context.getString(R.string.magnitude) to decimal(eclipse.magnitude, 2),
                context.getString(R.string.astronomy_altitude_peak) to degrees(moon.altitude),
                context.getString(R.string.astronomy_direction_peak) to direction(peakAzimuth)
            )

            showAdvancedData(context.getString(R.string.lunar_eclipse), advancedData)
        }
    }

    private fun getIcon(
        moon: CelestialObservation,
        shadow: LunarEclipseShadow,
        moonTilt: MoonTilt
    ): DrawableListIcon {
        return DrawableListIcon(
            LunarEclipseImageMapper(context).getEclipseImage(
                moon,
                shadow,
                imageSize,
                imageSize,
                moonTilt
            ).toDrawable(context.resources)
        )
    }

}
