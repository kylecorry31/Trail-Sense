package com.kylecorry.trail_sense.tools.solarpanel.domain

import com.kylecorry.sol.science.astronomy.AstronomyService
import com.kylecorry.sol.science.astronomy.IAstronomyService
import com.kylecorry.sol.science.astronomy.SolarPanelPosition
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.ITimeProvider
import com.kylecorry.trail_sense.shared.sensors.SystemTimeProvider
import java.time.Duration
import java.time.ZonedDateTime

class SolarPanelService(
    private val astronomy: IAstronomyService = AstronomyService(),
    private val timeProvider: ITimeProvider = SystemTimeProvider()
) {

    /**
     * Gets the solar radiation in kWh / m^2
     */
    fun getSolarEnergy(location: Coordinate, position: SolarPanelPosition): Float {
        return getSolarRadiationForRemainderOfDay(
            timeProvider.getTime(),
            location,
            position
        )
    }

    fun getBestPosition(
        state: SolarPanelState,
        location: Coordinate
    ): SolarPanelPosition {
        return when (state) {
            SolarPanelState.Now -> getBestPositionForNow(location)
            SolarPanelState.Today -> getBestPositionForToday(location)
        }
    }

    private fun getSolarRadiationForRemainderOfDay(
        start: ZonedDateTime,
        location: Coordinate,
        position: SolarPanelPosition
    ): Float {
        var time = start
        val date = time.toLocalDate()
        var total = 0.0
        val dt = Duration.ofMinutes(15).seconds

        while (time.toLocalDate() == date) {
            val radiation = astronomy.getSolarRadiation(time, location, position)
            if (radiation > 0) {
                total += dt / 3600.0 * radiation
            }
            time = time.plusSeconds(dt)
        }

        return total.toFloat()
    }

    private fun getBestPositionForNow(location: Coordinate): SolarPanelPosition {
        return astronomy.getBestSolarPanelPositionForTime(timeProvider.getTime(), location)
    }

    private fun getBestPositionForToday(location: Coordinate): SolarPanelPosition {
        return astronomy.getBestSolarPanelPositionForDay(
            timeProvider.getTime(),
            location
        )
    }

}