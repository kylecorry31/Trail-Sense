package com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance

import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator

class BeaconGuidanceTargetCoordinator(private val navigator: Navigator) {

    private var lastGuidanceTarget: ARGuidanceTarget? = null

    fun onTargetChanged(target: ARGuidanceTarget?) {
        if (lastGuidanceTarget == target) {
            return
        }

        val previousBeaconId = (lastGuidanceTarget as? BeaconGuidanceTarget)?.beacon?.id
        val newBeacon = (target as? BeaconGuidanceTarget)?.beacon

        if (previousBeaconId != null && previousBeaconId != newBeacon?.id) {
            navigator.cancelBeaconNavigation()
        }

        lastGuidanceTarget = target

        if (newBeacon != null) {
            navigator.navigateTo(newBeacon)
        }
    }

}
