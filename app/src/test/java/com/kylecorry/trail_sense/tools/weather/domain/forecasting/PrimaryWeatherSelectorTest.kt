package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.trail_sense.tools.weather.domain.ForecastCondition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class PrimaryWeatherSelectorTest {

    @ParameterizedTest
    @MethodSource("provideConditions")
    fun selectsTheMostImportantLikelyCondition(
        conditions: List<ForecastCondition>,
        expected: WeatherCondition?
    ) {
        assertEquals(expected, PrimaryWeatherSelector().getWeather(conditions))
    }

    companion object {
        private fun condition(condition: WeatherCondition, relativeProbability: Float? = null): ForecastCondition {
            return ForecastCondition(condition, relativeProbability = relativeProbability)
        }

        @JvmStatic
        fun provideConditions(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(emptyList<ForecastCondition>(), null),
                Arguments.of(
                    listOf(condition(WeatherCondition.Overcast), condition(WeatherCondition.Thunderstorm)),
                    WeatherCondition.Thunderstorm
                ),
                Arguments.of(
                    listOf(condition(WeatherCondition.Overcast, 1.9f), condition(WeatherCondition.Rain, 1f)),
                    WeatherCondition.Rain
                ),
                Arguments.of(
                    listOf(condition(WeatherCondition.Overcast, 2.5f), condition(WeatherCondition.Rain, 1f)),
                    WeatherCondition.Overcast
                ),
                Arguments.of(
                    listOf(condition(WeatherCondition.Precipitation, 1.2f), condition(WeatherCondition.Storm, 0.9f)),
                    WeatherCondition.Storm
                ),
                Arguments.of(
                    listOf(condition(WeatherCondition.Precipitation, 1.4f), condition(WeatherCondition.Wind, 2f)),
                    WeatherCondition.Wind
                ),
                Arguments.of(
                    listOf(condition(WeatherCondition.Precipitation, 1.2f), condition(WeatherCondition.Snow, 1.2f)),
                    WeatherCondition.Snow
                )
            )
        }
    }
}
