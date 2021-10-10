package com.kylecorry.trail_sense.weather.domain.clouds

import com.kylecorry.sol.science.meteorology.Precipitation
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.science.meteorology.clouds.ICloudService
import com.kylecorry.trail_sense.shared.domain.Probability
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

internal class CloudServiceTest {

    private lateinit var service: CloudService
    private lateinit var baseService: ICloudService

    @BeforeEach
    fun setup(){
        baseService = mock()
        whenever(baseService.getPrecipitationChance(CloudGenus.Cumulonimbus)).thenReturn(1f)
        whenever(baseService.getPrecipitationChance(CloudGenus.Stratus)).thenReturn(0.75f)
        whenever(baseService.getPrecipitationChance(CloudGenus.Cumulus)).thenReturn(0.5f)
        whenever(baseService.getPrecipitationChance(CloudGenus.Altostratus)).thenReturn(0.2f)
        whenever(baseService.getPrecipitationChance(CloudGenus.Cirrus)).thenReturn(0f)
        whenever(baseService.getPrecipitation(CloudGenus.Cumulus)).thenReturn(listOf(Precipitation.Snow, Precipitation.Rain))
        whenever(baseService.getPrecipitation(CloudGenus.Cumulonimbus)).thenReturn(listOf(Precipitation.Hail, Precipitation.Lightning))
        service = CloudService(baseService)
    }

    @ParameterizedTest
    @MethodSource("providePrecipitationProbability")
    fun getPrecipitationProbability(genus: CloudGenus, expected: Probability) {
        assertEquals(expected, service.getPrecipitationProbability(genus))
    }

    @Test
    fun getPrecipitation() {
        assertEquals(listOf(Precipitation.Snow, Precipitation.Rain), service.getPrecipitation(CloudGenus.Cumulus))
        assertEquals(listOf(Precipitation.Hail, Precipitation.Lightning), service.getPrecipitation(CloudGenus.Cumulonimbus))
    }

    companion object {

        @JvmStatic
        fun providePrecipitationProbability(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(CloudGenus.Cirrus, Probability.Never),
                Arguments.of(CloudGenus.Altostratus, Probability.Low),
                Arguments.of(CloudGenus.Cumulus, Probability.Moderate),
                Arguments.of(CloudGenus.Stratus, Probability.High),
                Arguments.of(CloudGenus.Cumulonimbus, Probability.Always),
            )
        }
    }
}