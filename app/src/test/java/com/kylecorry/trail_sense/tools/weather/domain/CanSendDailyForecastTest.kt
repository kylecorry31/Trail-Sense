package com.kylecorry.trail_sense.tools.weather.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalTime

class CanSendDailyForecastTest {
    @ParameterizedTest
    @CsvSource(
        "0, 0, 0, 0, true",
        "0, 0, 3, 0, true",
        "0, 0, 3, 1, false",
        "12, 0, 12, 0, true",
        "12, 0, 15, 0, true",
        "12, 0, 16, 0, false",
        "22, 0, 22, 0, true",
        "22, 0, 1, 0, true",
        "22, 0, 2, 0, false",
        "0, 30, 3, 30, true",
        "0, 30, 3, 31, false",
    )
    fun isSatisfiedBy(startHour: Int, startMinute: Int, currentHour: Int, currentMinute: Int, shouldAlert: Boolean) {
        val canSendDailyForecast = CanSendDailyForecast(LocalTime.of(startHour, startMinute))
        assertEquals(shouldAlert, canSendDailyForecast.isSatisfiedBy(LocalTime.of(currentHour, currentMinute)))
    }

}
