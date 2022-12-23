package com.kylecorry.trail_sense.weather.domain.forecasting.alerts

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.weather.domain.*
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class WeatherAlertGeneratorTest {

    @ParameterizedTest
    @MethodSource("provideAlerts")
    fun getAlerts(
        conditions: List<WeatherCondition>,
        high: Float,
        low: Float,
        alerts: List<WeatherAlert>
    ) {
        val weather = weather(conditions, high, low)
        val generator = WeatherAlertGenerator()
        val actual = generator.getAlerts(weather)
        assertEquals(alerts, actual)
    }

    private fun weather(
        conditions: List<WeatherCondition>,
        high: Float,
        low: Float
    ): CurrentWeather {
        return CurrentWeather(
            WeatherPrediction(
                conditions,
                emptyList(),
                null,
                HourlyArrivalTime.Now,
                TemperaturePrediction(
                    Temperature.zero,
                    Temperature.celsius(high),
                    Temperature.celsius(low),
                    Temperature.zero
                ),
                emptyList()
            ),
            PressureTendency(PressureCharacteristic.Steady, 0f),
            null,
            null
        )
    }

    companion object {
        @JvmStatic
        fun provideAlerts(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    listOf(WeatherCondition.Precipitation),
                    10f,
                    20f,
                    emptyList<WeatherAlert>()
                ),
                Arguments.of(listOf(WeatherCondition.Storm), 10f, 20f, listOf(WeatherAlert.Storm)),
                Arguments.of(
                    listOf(WeatherCondition.Clear),
                    WeatherSubsystem.COLD,
                    20f,
                    listOf(WeatherAlert.Cold)
                ),
                Arguments.of(
                    listOf(WeatherCondition.Clear),
                    10f,
                    WeatherSubsystem.HOT,
                    listOf(WeatherAlert.Hot)
                ),
                Arguments.of(
                    listOf(WeatherCondition.Storm),
                    WeatherSubsystem.COLD,
                    WeatherSubsystem.HOT,
                    listOf(WeatherAlert.Storm, WeatherAlert.Cold, WeatherAlert.Hot)
                ),
            )
        }
    }
}