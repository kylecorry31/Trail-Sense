package com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.IBeaconService
import junit.framework.Assert.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

internal class BeaconDistanceCalculatorFactoryTest {

    @Test
    fun getCalculator() {
        // Arrange
        val service = mock<IBeaconService>()
        val factory = BeaconDistanceCalculatorFactory(service)

        // Act
        val beaconCalculator = factory.getCalculator(Beacon(1, "", Coordinate.zero))
        val beaconGroupCalculator = factory.getCalculator(BeaconGroup(1, ""))

        // Assert
        assertTrue(beaconCalculator is BeaconDistanceCalculator)
        assertTrue(beaconGroupCalculator is BeaconGroupDistanceCalculator)
    }
}