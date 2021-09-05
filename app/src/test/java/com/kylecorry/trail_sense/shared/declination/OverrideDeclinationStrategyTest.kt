package com.kylecorry.trail_sense.shared.declination

import com.kylecorry.trail_sense.settings.infrastructure.IDeclinationPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class OverrideDeclinationStrategyTest {

    @Test
    fun getDeclination() {
        val prefs = mock<IDeclinationPreferences>()
        val strategy = OverrideDeclinationStrategy(prefs)

        whenever(prefs.declinationOverride).thenReturn(1.0f)
        assertEquals(1.0f, strategy.getDeclination(), 0.0f)

        whenever(prefs.declinationOverride).thenReturn(2.0f)
        assertEquals(2.0f, strategy.getDeclination(), 0.0f)
    }
}