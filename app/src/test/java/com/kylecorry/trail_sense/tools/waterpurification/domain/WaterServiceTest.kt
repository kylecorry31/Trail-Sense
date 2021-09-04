package com.kylecorry.trail_sense.tools.waterpurification.domain

import com.kylecorry.andromeda.core.units.Distance
import com.kylecorry.andromeda.core.units.DistanceUnits
import org.junit.Assert.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.util.stream.Stream

internal class WaterServiceTest {

    @ParameterizedTest
    @MethodSource("providePurificationTimes")
    fun getPurificationTime(altitude: Distance?, duration: Duration) {
        val service = WaterService()
        val time = service.getPurificationTime(altitude)
        assertEquals(time, duration)
    }

    companion object {
        @JvmStatic
        fun providePurificationTimes(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(Distance(0f, DistanceUnits.Meters), Duration.ofMinutes(1)),
                Arguments.of(Distance(999f, DistanceUnits.Meters), Duration.ofMinutes(1)),
                Arguments.of(Distance(1000f, DistanceUnits.Meters), Duration.ofMinutes(3)),
                Arguments.of(Distance(2000f, DistanceUnits.Meters), Duration.ofMinutes(3)),
                Arguments.of(null, Duration.ofMinutes(3)),
            )
        }
    }
}