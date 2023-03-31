package com.kylecorry.trail_sense.navigation.ui.data

import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.shared.commands.CoroutineValueCommand
import java.time.LocalDate

class NavAstronomyDataCommand(private val gps: IGPS) : CoroutineValueCommand<NavAstronomyData> {
    val astronomy = AstronomyService()

    override suspend fun execute(): NavAstronomyData = onDefault {
        NavAstronomyData(
            astronomy.getSunAzimuth(gps.location),
            astronomy.getMoonAzimuth(gps.location),
            astronomy.isSunUp(gps.location),
            astronomy.isMoonUp(gps.location),
            astronomy.getMoonPhase(LocalDate.now()).phase
        )
    }
}