package com.kylecorry.trail_sense.tools.solarpanel.domain

import com.kylecorry.sol.science.astronomy.AstronomyService
import com.kylecorry.sol.science.astronomy.IAstronomyService
import com.kylecorry.sol.science.astronomy.SolarPanelPosition
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.ITimeProvider
import com.kylecorry.trail_sense.shared.sensors.SystemTimeProvider

class SolarPanelService(
    private val astronomy: IAstronomyService = AstronomyService(),
    private val timeProvider: ITimeProvider = SystemTimeProvider()
) {

    fun getBestPosition(
        state: SolarPanelState,
        location: Coordinate
    ): SolarPanelPosition {
        return when (state) {
            SolarPanelState.Now -> getBestPositionForTime(location)
            SolarPanelState.Today -> getBestPositionForDay(location)
        }
    }

    private fun getBestPositionForTime(location: Coordinate): SolarPanelPosition {
        return astronomy.getBestSolarPanelPositionForTime(timeProvider.getTime(), location)
    }

    private fun getBestPositionForDay(location: Coordinate): SolarPanelPosition {
        return astronomy.getBestSolarPanelPositionForDay(
            timeProvider.getTime(),
            location
        )
    }

}