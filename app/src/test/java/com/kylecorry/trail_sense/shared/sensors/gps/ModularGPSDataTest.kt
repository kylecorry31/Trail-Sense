package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class ModularGPSDataTest {
    @Test
    fun preservesSpeedSourceWhenCopyingOrPopulatingFromData() {
        for (source in SpeedSource.entries) {
            val original = ModularGPSData().apply { speedSource = source }
            val copy = ModularGPSData()
            original.copyInto(copy)
            assertEquals(source, copy.speedSource)
            copy.populateFromGPS(original)
            assertEquals(source, copy.speedSource)
        }
    }

    @Test
    fun marksExternalGPSAsProvider() {
        val source = ModularGPSData()
        val gps = mock<ISatelliteGPS>(defaultAnswer = org.mockito.AdditionalAnswers.delegatesTo(source))
        val data = ModularGPSData().apply { speedSource = SpeedSource.PositionDerived }
        data.populateFromGPS(gps)
        assertEquals(SpeedSource.Provider, data.speedSource)
    }
}
