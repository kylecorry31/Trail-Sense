package com.kylecorry.trail_sense.tools.navigation.ui.data

import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.navigation.ui.IMappableReferencePoint
import com.kylecorry.trail_sense.tools.navigation.ui.MappableReferencePoint
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.MarkerCompassLayer

class UpdateAstronomyLayerCommand(
    private val layer: MarkerCompassLayer,
    private val prefs: UserPreferences,
    private val gps: IGPS,
    private val declination: () -> Float
) : CoroutineCommand {
    override suspend fun execute() = onDefault {
        val markers = mutableListOf<IMappableReferencePoint>()
        val astro = NavAstronomyDataCommand(gps).execute()

        if (prefs.astronomy.showOnCompass) {
            val showWhenDown = prefs.astronomy.showOnCompassWhenDown

            if (astro.isSunUp) {
                markers.add(
                    MappableReferencePoint(
                        1,
                        R.drawable.ic_sun,
                        fromTrueNorth(astro.sunBearing)
                    )
                )
            } else if (showWhenDown) {
                markers.add(
                    MappableReferencePoint(
                        1,
                        R.drawable.ic_sun,
                        fromTrueNorth(astro.sunBearing),
                        opacity = 0.5f
                    )
                )
            }


            if (astro.isMoonUp) {
                markers.add(
                    MappableReferencePoint(
                        2,
                        MoonPhaseImageMapper().getPhaseImage(astro.moonPhase),
                        fromTrueNorth(astro.moonBearing),
                        rotation = astro.moonTilt
                    )
                )
            } else if (showWhenDown) {
                markers.add(
                    MappableReferencePoint(
                        2,
                        MoonPhaseImageMapper().getPhaseImage(astro.moonPhase),
                        fromTrueNorth(astro.moonBearing),
                        opacity = 0.5f,
                        rotation = astro.moonTilt
                    )
                )
            }
        }

        layer.setMarkers(markers)
    }

    private fun fromTrueNorth(bearing: Bearing): Bearing {
        return if (prefs.compass.useTrueNorth) {
            bearing
        } else {
            DeclinationUtils.fromTrueNorthBearing(bearing, declination())
        }
    }
}