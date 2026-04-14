package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.sol.science.astronomy.Astronomy
import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.sol.science.astronomy.meteors.MeteorShower
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.readableName
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.astronomy.ui.format.PlanetMapper
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.SphericalARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import java.time.ZonedDateTime

sealed interface AstronomySelection {
    data object Sun : AstronomySelection
    data object Moon : AstronomySelection
    data class PlanetTarget(val planet: Planet) : AstronomySelection
    data class StarTarget(val star: Star) : AstronomySelection
    data class MeteorShowerTarget(val shower: MeteorShower) : AstronomySelection
}

class AstronomyGuidanceTarget(
    private val astronomyService: AstronomyService,
    private val selection: AstronomySelection,
    private val getDisplayedTime: () -> ZonedDateTime
) : ARGuidanceTarget {
    override suspend fun refresh(view: AugmentedRealityView): ARGuidanceTargetState = onDefault {
        val location = view.location
        val time = getDisplayedTime()
        val planetMapper = PlanetMapper(view.context)

        when (selection) {
            AstronomySelection.Sun -> {
                ARGuidanceTargetState(
                    ARGuidanceDisplayState(
                        view.context.getString(R.string.sun),
                        R.drawable.ic_sun
                    ),
                    SphericalARPoint(
                        astronomyService.getSunAzimuth(location, time).value,
                        astronomyService.getSunAltitude(location, time),
                        isTrueNorth = true
                    )
                )
            }

            AstronomySelection.Moon -> {
                val phase = Astronomy.getMoonPhase(time)
                ARGuidanceTargetState(
                    ARGuidanceDisplayState(
                        view.context.getString(R.string.moon),
                        MoonPhaseImageMapper().getPhaseImage(phase.phase),
                        iconRotation = astronomyService.getMoonTilt(location, time)
                    ),
                    SphericalARPoint(
                        astronomyService.getMoonAzimuth(location, time).value,
                        astronomyService.getMoonAltitude(location, time),
                        isTrueNorth = true
                    )
                )
            }

            is AstronomySelection.PlanetTarget -> {
                val observation = astronomyService.getVisiblePlanets(
                    location,
                    time,
                    thresholdElevation = null,
                    includeDimPlanets = true
                ).firstOrNull { it.first == selection.planet }?.second
                    ?: Astronomy.getPlanetPosition(
                        selection.planet,
                        time,
                        location,
                        withRefraction = true
                    )
                ARGuidanceTargetState(
                    ARGuidanceDisplayState(
                        planetMapper.getName(selection.planet),
                        planetMapper.getImage(selection.planet)
                    ),
                    SphericalARPoint(
                        observation.azimuth.value,
                        observation.altitude,
                        isTrueNorth = true
                    )
                )
            }

            is AstronomySelection.StarTarget -> {
                val position = astronomyService.getStarPosition(selection.star, location, time)
                ARGuidanceTargetState(
                    ARGuidanceDisplayState(
                        "${view.context.getString(R.string.star)}: ${selection.star.name}",
                        R.drawable.ic_star
                    ),
                    SphericalARPoint(
                        position.azimuth.value,
                        position.altitude,
                        isTrueNorth = true
                    )
                )
            }

            is AstronomySelection.MeteorShowerTarget -> {
                val position = Astronomy.getMeteorShowerPosition(
                    selection.shower,
                    location,
                    time.toInstant()
                )
                ARGuidanceTargetState(
                    ARGuidanceDisplayState(
                        selection.shower.readableName(),
                        R.drawable.ic_meteor
                    ),
                    SphericalARPoint(
                        position.azimuth.value,
                        position.altitude,
                        isTrueNorth = true
                    )
                )
            }
        }
    }
}
