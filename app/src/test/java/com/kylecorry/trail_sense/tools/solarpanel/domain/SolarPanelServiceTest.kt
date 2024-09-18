package com.kylecorry.trail_sense.tools.solarpanel.domain

import com.kylecorry.andromeda.core.time.ITimeProvider
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.ZonedDateTime

class SolarPanelServiceTest {

    @ParameterizedTest
    @CsvSource(
        // Northern hemisphere (Winter)
        "2024-02-11T00:00:00-05:00, PT24H, 42.0, -72.0, true, 59.0, 180.0",
        "2024-02-11T00:00:00-05:00, PT48H, 42.0, -72.0, true, 59.0, 180.0",
        "2024-02-11T00:00:00-05:00, PT48H, 42.0, -72.0, false, 58.9, 180.0",
        // Northern hemisphere (Summer)
        "2024-07-01T00:00:00-05:00, PT24H, 42.0, -72.0, true, 7.0, 180.0",
        "2024-07-01T12:00:00-05:00, PT24H, 42.0, -72.0, true, 42.0, 263.0",
        "2024-07-01T12:00:00-05:00, PT48H, 42.0, -72.0, false, 3.0, 115",
        // Northern hemisphere (night)
        "2024-02-11T20:00:00-05:00, PT24H, 42.0, -72.0, true, 79.0, 108.0",
        "2024-02-11T20:00:00-05:00, PT24H, 42.0, -72.0, false, 58.7, 180.1",

        // Southern hemisphere (Winter)
        "2024-07-01T00:00:00-05:00, PT24H, -42.0, 72.0, true, 68.0, 359.9",
        "2024-07-01T12:00:00-05:00, PT24H, -42.0, 72.0, true, 78.0, 41.9",
        "2024-07-01T12:00:00-05:00, PT48H, -42.0, 72.0, false, 68.3, 0.0",
        // Southern hemisphere (Summer)
        "2024-02-11T00:00:00-05:00, PT24H, -42.0, 72.0, true, 21.0, 359.9",
        "2024-02-11T00:00:00-05:00, PT48H, -42.0, 72.0, true, 21.0, 359.9",
        "2024-02-11T00:00:00-05:00, PT48H, -42.0, 72.0, false, 21.5, 359.9",
        // Southern hemisphere (night)
        "2024-07-01T20:00:00-05:00, PT24H, -42.0, 72.0, true, 78.0, 41.9",
        "2024-07-01T20:00:00-05:00, PT24H, -42.0, 72.0, false, 68.3, 359.9"
    )
    fun getBestPosition(
        timeString: String,
        durationString: String,
        latitude: Double,
        longitude: Double,
        restrictToToday: Boolean,
        expectedTilt: Float,
        expectedAzimuth: Float
    ) {
        val timeProvider = mock<ITimeProvider>()
        val service = SolarPanelService(timeProvider)
        val time = ZonedDateTime.parse(timeString)
        whenever(timeProvider.getTime()).thenReturn(time)
        val duration = Duration.parse(durationString)
        val location = Coordinate(latitude, longitude)
        val (tilt, azimuth) = service.getBestPosition(location, duration, restrictToToday)
        assertEquals(expectedTilt, tilt, 1f)
        assertEquals(expectedAzimuth, azimuth.value, 1f)
    }

    @ParameterizedTest
    @CsvSource(
        // Northern hemisphere (Winter)
        "2024-02-11T00:00:00-05:00, PT24H, 42.0, -72.0, true, 59.0, 180.0, 5.2",
        "2024-02-11T00:00:00-05:00, PT48H, 42.0, -72.0, true, 59.0, 180.0, 5.2",
        "2024-02-11T00:00:00-05:00, PT48H, 42.0, -72.0, false, 58.9, 180.0, 10.5",
        // Northern hemisphere (Summer)
        "2024-07-01T00:00:00-05:00, PT24H, 42.0, -72.0, true, 7.0, 180.0, 7.2",
        "2024-07-01T12:00:00-05:00, PT24H, 42.0, -72.0, true, 42.0, 263.0, 4.7",
        "2024-07-01T12:00:00-05:00, PT48H, 42.0, -72.0, false, 4.4, 129.6, 14.4",
        // Northern hemisphere (night)
        "2024-02-11T20:00:00-05:00, PT24H, 42.0, -72.0, true, 79.0, 108.0, 0.0",
        "2024-02-11T20:00:00-05:00, PT24H, 42.0, -72.0, false, 58.7, 180.1, 5.2",

        // Southern hemisphere (Winter)
        "2024-07-01T00:00:00-05:00, PT24H, -42.0, 72.0, true, 68.0, 359.9, 4.0",
        "2024-07-01T12:00:00-05:00, PT24H, -42.0, 72.0, true, 78.0, 41.9, 0.8",
        "2024-07-01T12:00:00-05:00, PT48H, -42.0, 72.0, false, 68.3, 359.9, 8.0",
        // Southern hemisphere (Summer)
        "2024-02-11T00:00:00-05:00, PT24H, -42.0, 72.0, true, 21.0, 359.9, 7.0",
        "2024-02-11T00:00:00-05:00, PT48H, -42.0, 72.0, true, 21.0, 359.9, 7.0",
        "2024-02-11T00:00:00-05:00, PT48H, -42.0, 72.0, false, 21.5, 359.9, 13.9",
        // Southern hemisphere (night)
        "2024-07-01T20:00:00-05:00, PT24H, -42.0, 72.0, true, 78.0, 41.9, 0.8",
        "2024-07-01T20:00:00-05:00, PT24H, -42.0, 72.0, false, 68.3, 359.9, 4.0"
    )
    fun getSolarEnergy(
        timeString: String,
        durationString: String,
        latitude: Double,
        longitude: Double,
        restrictToToday: Boolean,
        tilt: Float,
        azimuth: Float,
        expectedEnergy: Float
    ) {
        val timeProvider = mock<ITimeProvider>()
        val service = SolarPanelService(timeProvider)
        val time = ZonedDateTime.parse(timeString)
        whenever(timeProvider.getTime()).thenReturn(time)
        val duration = Duration.parse(durationString)
        val location = Coordinate(latitude, longitude)
        val energy = service.getSolarEnergy(location, tilt, Bearing(azimuth), duration, restrictToToday)
        assertEquals(expectedEnergy, energy, 0.1f)
    }
}