package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions

class BeaconGuidanceTargetCoordinatorTest {

    private val navigator = mock<Navigator>()
    private val coordinator = BeaconGuidanceTargetCoordinator(navigator)
    private val beacon = Beacon(1, "Target", Coordinate(42.0, -72.0))

    @Test
    fun nonBeaconTargetsDoNotChangeNavigation() {
        coordinator.onTargetChanged(null)
        coordinator.onTargetChanged(mock<ARGuidanceTarget>())
        coordinator.onTargetChanged(null)
        verifyNoInteractions(navigator)
    }

    @Test
    fun equivalentBeaconTargetsOnlyNavigateOnce() {
        coordinator.onTargetChanged(BeaconGuidanceTarget(beacon))
        coordinator.onTargetChanged(BeaconGuidanceTarget(beacon.copy()))
        verify(navigator).navigateTo(beacon)
        verifyNoMoreInteractions(navigator)
    }

    @Test
    fun switchingBeaconsReplacesNavigation() {
        val second = beacon.copy(id = 2)
        coordinator.onTargetChanged(BeaconGuidanceTarget(beacon))
        coordinator.onTargetChanged(BeaconGuidanceTarget(second))
        inOrder(navigator) {
            verify(navigator).navigateTo(beacon)
            verify(navigator).navigateTo(second)
        }
        verifyNoMoreInteractions(navigator)
    }

    @Test
    fun updatingSameBeaconDoesNotCancelNavigation() {
        val updated = beacon.copy(name = "Renamed")
        coordinator.onTargetChanged(BeaconGuidanceTarget(beacon))
        coordinator.onTargetChanged(BeaconGuidanceTarget(updated))
        verify(navigator).navigateTo(beacon)
        verify(navigator).navigateTo(updated)
        verifyNoMoreInteractions(navigator)
    }

    @Test
    fun clearingBeaconPreservesNavigationAndAllowsReselection() {
        coordinator.onTargetChanged(BeaconGuidanceTarget(beacon))
        coordinator.onTargetChanged(null)
        coordinator.onTargetChanged(null)
        verify(navigator).navigateTo(beacon)
        verifyNoMoreInteractions(navigator)

        coordinator.onTargetChanged(BeaconGuidanceTarget(beacon))
        verify(navigator, times(2)).navigateTo(beacon)
        verifyNoMoreInteractions(navigator)
    }

    @Test
    fun switchingToNonBeaconTargetPreservesBeaconNavigation() {
        coordinator.onTargetChanged(BeaconGuidanceTarget(beacon))
        coordinator.onTargetChanged(mock<ARGuidanceTarget>())
        coordinator.onTargetChanged(null)
        verify(navigator).navigateTo(beacon)
        verifyNoMoreInteractions(navigator)
    }
}
