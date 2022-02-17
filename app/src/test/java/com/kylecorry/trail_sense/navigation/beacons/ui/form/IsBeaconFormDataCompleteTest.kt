package com.kylecorry.trail_sense.navigation.beacons.ui.form

import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class IsBeaconFormDataCompleteTest {

    @ParameterizedTest
    @MethodSource("provideData")
    fun isSatisfiedByCompletedForm(data: CreateBeaconData, expected: Boolean) {
        val spec = IsBeaconFormDataComplete()
        assertEquals(expected, spec.isSatisfiedBy(data))
    }

    companion object {
        @JvmStatic
        fun provideData(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    CreateBeaconData(
                        name = "Name",
                        coordinate = Coordinate.zero,
                        createAtDistance = true,
                        distanceTo = Distance.meters(0f),
                        bearingTo = Bearing(0f)
                    ),
                    true
                ),
                Arguments.of(
                    CreateBeaconData(
                        name = "Name",
                        coordinate = Coordinate.zero,
                        createAtDistance = false
                    ),
                    true
                ),
                Arguments.of(
                    CreateBeaconData(
                        name = "",
                        coordinate = Coordinate.zero,
                        createAtDistance = false
                    ),
                    false
                ),
                Arguments.of(
                    CreateBeaconData(
                        name = "Name",
                        coordinate = Coordinate.zero,
                        createAtDistance = true,
                        distanceTo = Distance.meters(0f),
                        bearingTo = null
                    ),
                    false
                ),
                Arguments.of(
                    CreateBeaconData(
                        name = "Name",
                        coordinate = Coordinate.zero,
                        createAtDistance = true,
                        distanceTo = null,
                        bearingTo = Bearing(0f)
                    ),
                    false
                ),
                Arguments.of(
                    CreateBeaconData(
                        name = "Name",
                        coordinate = null,
                        createAtDistance = false
                    ),
                    false
                ),
            )
        }
    }
}