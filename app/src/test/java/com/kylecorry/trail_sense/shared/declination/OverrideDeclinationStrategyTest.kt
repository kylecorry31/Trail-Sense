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
        whenever(prefs.declinationOverride).thenReturn(1.0f)

        val strategy = OverrideDeclinationStrategy(prefs)

        assertEquals(1.0f, strategy.getDeclination(), 0.0f)
    }
}