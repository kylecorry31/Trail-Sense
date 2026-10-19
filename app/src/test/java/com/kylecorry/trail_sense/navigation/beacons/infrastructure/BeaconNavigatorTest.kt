package com.kylecorry.trail_sense.tools.beacons.infrastructure

import android.os.Bundle
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigation.IAppNavigation
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.NavigatorFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class BeaconNavigatorTest {

    private lateinit var navigator: BeaconNavigator
    private lateinit var navigation: IAppNavigation
    private lateinit var service: IBeaconService
    private lateinit var appNavigator: Navigator
    private lateinit var bundle: Bundle

    @BeforeEach
    fun setup() {
        navigation = mock()
        service = mock()
        appNavigator = mock()
        bundle = mock()
        navigator = BeaconNavigator(service, navigation, appNavigator, Dispatchers.Default) { bundle }
    }

    @Test
    fun navigates() = runBlocking {
        val beacon = Beacon(2, "Test", Coordinate.zero)

        navigator.navigateTo(beacon)

        verify(appNavigator).navigateTo(2L)
        verify(bundle).putBoolean(NavigatorFragment.ARG_NAVIGATION_STARTED, true)
        verify(navigation).navigate(eq(R.id.action_navigation), eq(bundle))
    }

    @Test
    fun createsAndNavigates() = runBlocking {
        val beacon = Beacon(0, "Test", Coordinate.zero)

        whenever(service.add(beacon))
            .thenReturn(1)

        navigator.navigateTo(beacon)

        verify(service).add(beacon)
        verify(appNavigator).navigateTo(1L)
        verify(bundle).putBoolean(NavigatorFragment.ARG_NAVIGATION_STARTED, true)
        verify(navigation).navigate(eq(R.id.action_navigation), eq(bundle))

    }
}
