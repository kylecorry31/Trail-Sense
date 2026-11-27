package com.kylecorry.trail_sense.tools.weather.domain

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.sol.units.Reading
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Duration
import java.time.Instant

internal class PressureTideTest {

    private val midnightUtc = Instant.parse("2026-11-27T00:00:00Z")

    private fun at(utcHour: Double): Instant {
        return midnightUtc.plusSeconds((utcHour * 3600).toLong())
    }

    @ParameterizedTest
    @CsvSource(
        "0.0, 9.73, 1.16",
        "0.0, 21.73, 1.16",
        "0.0, 3.73, -1.16",
        "41.9, 9.73, 0.477",
        "-41.9, 9.73, 0.477",
        "90.0, 9.73, 0.0"
    )
    fun semidiurnalTidePeaksNearTenLocalSolarTime(latitude: Double, utcHour: Double, expected: Double) {
        assertEquals(expected, PressureTide.getSemidiurnalOffsetHpa(at(utcHour), Coordinate(latitude, 0.0)), 0.005)
    }

    @ParameterizedTest
    @CsvSource(
        "0.0, 5.2, 0.593",
        "0.0, 17.2, -0.593",
        "41.9, 5.2, 0.244",
        "90.0, 5.2, 0.0"
    )
    fun diurnalTidePeaksNearFiveLocalSolarTime(latitude: Double, utcHour: Double, expected: Double) {
        assertEquals(expected, PressureTide.getDiurnalOffsetHpa(at(utcHour), Coordinate(latitude, 0.0)), 0.005)
    }

    @ParameterizedTest
    @CsvSource(
        "90.0, 0.0, 11.07, 0.085",
        "90.0, 120.0, 11.07, 0.085",
        "0.0, -75.0, 11.07, -0.0425",
        "35.26, 0.0, 11.07, 0.0"
    )
    fun standingSemidiurnalTideFollowsUniversalTime(
        latitude: Double,
        longitude: Double,
        utcHour: Double,
        expected: Double
    ) {
        val offset = PressureTide.getStandingSemidiurnalOffsetHpa(at(utcHour), Coordinate(latitude, longitude))
        assertEquals(expected, offset, 0.001)
    }

    @Test
    fun migratingTidesFollowLocalSolarTime() {
        val greenwich = Coordinate(20.0, 0.0)
        val fourHoursWest = Coordinate(20.0, -60.0)
        val later = midnightUtc.plus(Duration.ofHours(4))

        assertEquals(
            PressureTide.getSemidiurnalOffsetHpa(midnightUtc, greenwich),
            PressureTide.getSemidiurnalOffsetHpa(later, fourHoursWest),
            0.0001
        )
        assertEquals(
            PressureTide.getDiurnalOffsetHpa(midnightUtc, greenwich),
            PressureTide.getDiurnalOffsetHpa(later, fourHoursWest),
            0.0001
        )
    }

    @Test
    fun offsetIsTheSumOfTheTides() {
        val location = Coordinate(25.0, -80.0)
        val time = at(15.5)

        val expected = PressureTide.getSemidiurnalOffsetHpa(time, location) +
            PressureTide.getStandingSemidiurnalOffsetHpa(time, location) +
            PressureTide.getDiurnalOffsetHpa(time, location)

        assertEquals(expected, PressureTide.getOffsetHpa(time, location).toDouble(), 0.0001)
    }

    @Test
    fun addingRestoresWhatWasRemoved() {
        val location = Coordinate(10.0, 30.0)
        (0 until 24).map {
            Reading(Pressure.from(30f, PressureUnits.Inhg), midnightUtc.plus(Duration.ofHours(it.toLong())))
        }.forEach { reading ->
            val restored = PressureTide.add(PressureTide.remove(reading, location), location)
            assertEquals(reading.time, restored.time)
            assertEquals(PressureUnits.Inhg, restored.value.units)
            assertEquals(reading.value.value, restored.value.value, 0.0001f)
        }
    }

    @Test
    fun removingTheTideFlattensATidalSignal() {
        val location = Coordinate(5.0, 100.0)
        (0 until 24).map {
            val time = midnightUtc.plus(Duration.ofHours(it.toLong()))
            Reading(Pressure.hpa(1010f + PressureTide.getOffsetHpa(time, location)), time)
        }.forEach {
            assertEquals(1010f, PressureTide.remove(it, location).value.hpa().value, 0.001f)
        }
    }
}
