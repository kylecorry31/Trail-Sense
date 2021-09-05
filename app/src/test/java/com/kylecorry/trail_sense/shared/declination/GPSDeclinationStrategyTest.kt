package com.kylecorry.trail_sense.shared.declination

import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.sol.science.geology.IGeologyService
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class GPSDeclinationStrategyTest {

    @Test
    fun getDeclination() {
        val geology = mock<IGeologyService>()
        val gps = mock<IGPS>()
        val strategy = GPSDeclinationStrategy(gps, geology)

        whenever(gps.location).thenReturn(Coordinate.zero)
        whenever(gps.altitude).thenReturn(0.0f)
        whenever(geology.getMagneticDeclination(eq(Coordinate.zero), eq(0.0f), any())).thenReturn(
            5.0f
        )

        assertEquals(5.0f, strategy.getDeclination())

        whenever(gps.location).thenReturn(Coordinate(1.0, 2.0))
        whenever(gps.altitude).thenReturn(10.0f)
        whenever(
            geology.getMagneticDeclination(
                eq(Coordinate(1.0, 2.0)),
                eq(10.0f),
                any()
            )
        ).thenReturn(10.0f)

        assertEquals(10.0f, strategy.getDeclination())
    }
}