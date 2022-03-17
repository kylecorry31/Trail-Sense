package com.kylecorry.trail_sense.navigation.beacons.infrastructure

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.shared.navigation.IAppNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

internal class BeaconNavigatorTest {

    private lateinit var navigator: BeaconNavigator
    private lateinit var navigation: IAppNavigation
    private lateinit var service: IBeaconService

    @BeforeEach
    fun setup() {
        navigation = mock()
        service = mock()
        navigator = BeaconNavigator(service, navigation, Dispatchers.Default)
    }

    @Test
    fun navigates() = runBlocking {
        val beacon = Beacon(2, "Test", Coordinate.zero)

        navigator.navigateTo(beacon)

        verify(navigation).navigate(
            eq(R.id.action_navigation),
            argThat { size == 1 && first().first == "destination" && first().second == 2L }
        )
    }

    @Test
    fun createsAndNavigates() = runBlocking {
        val beacon = Beacon(0, "Test", Coordinate.zero)

        whenever(service.add(beacon))
            .thenReturn(1)

        navigator.navigateTo(beacon)

        verify(service).add(beacon)
        verify(navigation).navigate(
            eq(R.id.action_navigation),
            argThat { size == 1 && first().first == "destination" && first().second == 1L }
        )

    }
}