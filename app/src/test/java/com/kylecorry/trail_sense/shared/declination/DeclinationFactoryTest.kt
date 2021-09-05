package com.kylecorry.trail_sense.shared.declination

import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.trail_sense.settings.infrastructure.IDeclinationPreferences
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

internal class DeclinationFactoryTest {

    @Test
    fun returnsGPSStrategyWhenAutoDeclination() {
        val prefs = mock(IDeclinationPreferences::class.java)
        val gps = mock(IGPS::class.java)
        whenever(prefs.useAutoDeclination).thenReturn(true)

        val provider = DeclinationFactory()

        val strategy = provider.getDeclinationStrategy(prefs, gps)

        assertTrue(strategy is GPSDeclinationStrategy)
    }

    @Test
    fun returnsOverrideStrategyWhenNotAutoDeclination() {
        val prefs = mock(IDeclinationPreferences::class.java)
        val gps = mock(IGPS::class.java)
        whenever(prefs.useAutoDeclination).thenReturn(false)

        val provider = DeclinationFactory()

        val strategy = provider.getDeclinationStrategy(prefs, gps)

        assertTrue(strategy is OverrideDeclinationStrategy)
    }

    @Test
    fun returnsOverrideStrategyWhenNoGPS() {
        val prefs = mock(IDeclinationPreferences::class.java)
        whenever(prefs.useAutoDeclination).thenReturn(true)

        val provider = DeclinationFactory()

        val strategy = provider.getDeclinationStrategy(prefs)

        assertTrue(strategy is OverrideDeclinationStrategy)
    }
}