package com.kylecorry.trail_sense.tools.weather.domain.forecasting.model

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

internal class LoggedSkyTest {

    @ParameterizedTest
    @MethodSource("provideClouds")
    fun splitsTheLoggedCloudsByAge(clouds: List<Reading<CloudGenus?>>, expected: LoggedSky) {
        assertEquals(expected, LoggedSky.fromLoggedClouds(clouds, now))
    }

    companion object {
        private val now = Instant.parse("2026-11-28T12:00:00Z")

        private fun log(minutesAgo: Long, vararg genera: CloudGenus?): List<Reading<CloudGenus?>> {
            return genera.map { Reading(it, now.minus(Duration.ofMinutes(minutesAgo))) }
        }

        private fun sky(cover: SkyCover, vararg clouds: CloudGenus): SkyLog {
            return SkyLog(cover, clouds.toSet())
        }

        @JvmStatic
        fun provideClouds(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(emptyList<Reading<CloudGenus?>>(), LoggedSky(null, null)),
                Arguments.of(log(300, CloudGenus.Stratus), LoggedSky(null, null)),
                Arguments.of(log(-10, CloudGenus.Stratus), LoggedSky(null, null)),
                Arguments.of(log(10, null), LoggedSky(sky(SkyCover.Clear), null)),
                Arguments.of(log(10, CloudGenus.Cumulus), LoggedSky(sky(SkyCover.PartlyCloudy, CloudGenus.Cumulus), null)),
                Arguments.of(log(10, CloudGenus.Cirrus), LoggedSky(sky(SkyCover.PartlyCloudy, CloudGenus.Cirrus), null)),
                Arguments.of(
                    log(10, CloudGenus.Cumulonimbus),
                    LoggedSky(sky(SkyCover.PartlyCloudy, CloudGenus.Cumulonimbus), null)
                ),
                Arguments.of(log(10, CloudGenus.Stratus), LoggedSky(sky(SkyCover.MostlyCloudy, CloudGenus.Stratus), null)),
                Arguments.of(
                    log(10, CloudGenus.Nimbostratus),
                    LoggedSky(sky(SkyCover.MostlyCloudy, CloudGenus.Nimbostratus), null)
                ),
                Arguments.of(
                    log(10, CloudGenus.Altostratus),
                    LoggedSky(sky(SkyCover.MostlyCloudy, CloudGenus.Altostratus), null)
                ),
                Arguments.of(
                    log(10, CloudGenus.Cirrostratus),
                    LoggedSky(sky(SkyCover.MostlyCloudy, CloudGenus.Cirrostratus), null)
                ),
                Arguments.of(
                    log(10, CloudGenus.Stratocumulus),
                    LoggedSky(sky(SkyCover.MostlyCloudy, CloudGenus.Stratocumulus), null)
                ),
                Arguments.of(
                    log(10, CloudGenus.Cumulus, CloudGenus.Altostratus),
                    LoggedSky(sky(SkyCover.MostlyCloudy, CloudGenus.Cumulus, CloudGenus.Altostratus), null)
                ),
                Arguments.of(
                    log(40, CloudGenus.Stratus) + log(10, null),
                    LoggedSky(sky(SkyCover.Clear, CloudGenus.Stratus), null)
                ),
                Arguments.of(
                    log(200, CloudGenus.Cirrus) + log(120, CloudGenus.Cirrostratus) + log(20, CloudGenus.Altostratus),
                    LoggedSky(
                        sky(SkyCover.MostlyCloudy, CloudGenus.Altostratus),
                        sky(SkyCover.MostlyCloudy, CloudGenus.Cirrus, CloudGenus.Cirrostratus)
                    )
                ),
                Arguments.of(
                    log(90, CloudGenus.Cumulus),
                    LoggedSky(null, sky(SkyCover.PartlyCloudy, CloudGenus.Cumulus))
                )
            )
        }
    }
}
