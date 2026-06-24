package com.kylecorry.trail_sense.tools.navigation.ui.data

import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.trail_sense.shared.commands.CoroutineValueCommand
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import java.time.LocalDate

class NavAstronomyDataCommand(private val gps: IGPS) : CoroutineValueCommand<NavAstronomyData> {
    val astronomy = AstronomyService()

    override suspend fun execute(): NavAstronomyData = onDefault {
        val moonPosition = astronomy.getMoonPosition(gps.location)
        val sunPosition = astronomy.getSunPosition(gps.location)
        NavAstronomyData(
            sunPosition.azimuth.value,
            moonPosition.azimuth.value,
            astronomy.isSunUp(gps.location),
            astronomy.isMoonUp(gps.location),
            astronomy.getMoonPhase(LocalDate.now()).phaseAngle,
            astronomy.getMoonTilt(gps.location)
        )
    }
}
