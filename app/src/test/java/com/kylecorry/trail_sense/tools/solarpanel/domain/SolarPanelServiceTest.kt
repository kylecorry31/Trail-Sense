package com.kylecorry.trail_sense.tools.solarpanel.domain

import com.kylecorry.sol.science.astronomy.IAstronomyService
import com.kylecorry.sol.science.astronomy.SolarPanelPosition
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.sensors.ITimeProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.ZoneId
import java.time.ZonedDateTime

internal class SolarPanelServiceTest {

    @Test
    fun getBestPositionForTime() {
        val astronomy = mock<IAstronomyService>()
        val timeProvider = mock<ITimeProvider>()
        val service = SolarPanelService(astronomy, timeProvider)
        val location = Coordinate.zero
        val time = ZonedDateTime.of(2020, 9, 6, 0, 0, 0, 0, ZoneId.of("UTC"))

        whenever(
            astronomy.getBestSolarPanelPositionForTime(
                eq(time),
                eq(location)
            )
        ).thenReturn(
            SolarPanelPosition(1f, Bearing(10f))
        )

        whenever(timeProvider.getTime()).thenReturn(time)

        val actual = service.getBestPosition(SolarPanelState.Now, location)

        assertEquals(1f, actual.tilt, 0.0f)
        assertEquals(10f, actual.bearing.value, 0.0f)
    }

    @Test
    fun getBestPositionForDay() {
        val astronomy = mock<IAstronomyService>()
        val timeProvider = mock<ITimeProvider>()
        val service = SolarPanelService(astronomy, timeProvider)
        val location = Coordinate.zero
        val time = ZonedDateTime.of(2020, 9, 6, 0, 0, 0, 0, ZoneId.of("UTC"))

        whenever(
            astronomy.getBestSolarPanelPositionForDay(
                eq(time),
                eq(location)
            )
        ).thenReturn(
            SolarPanelPosition(1f, Bearing(10f))
        )

        whenever(timeProvider.getTime()).thenReturn(time)


        val actual = service.getBestPosition(SolarPanelState.Today, location)

        assertEquals(1f, actual.tilt, 0.0f)
        assertEquals(10f, actual.bearing.value, 0.0f)
    }
}