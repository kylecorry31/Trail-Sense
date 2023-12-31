package com.kylecorry.trail_sense.tools.augmented_reality.guide

import com.kylecorry.trail_sense.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.augmented_reality.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.position.GeographicARPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NavigationARGuide(private val navigator: Navigator) : ARGuide {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    override fun start(arView: AugmentedRealityView) {
        job?.cancel()
        job = scope.launch {
            navigator.destination.collect {
                if (it == null) {
                    arView.clearGuide()
                } else {
                    arView.guideTo(GeographicARPoint(it.coordinate, it.elevation)) {
                        // Do nothing when reached
                    }
                }
            }
        }
    }

    override fun stop(arView: AugmentedRealityView) {
        job?.cancel()
        arView.clearGuide()
    }
}