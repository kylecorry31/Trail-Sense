package com.kylecorry.trail_sense.shared.extensions

import androidx.annotation.LayoutRes
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.fragments.XmlReactiveFragment
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import java.time.Duration

abstract class TrailSenseReactiveFragment(@LayoutRes private val layoutId: Int) :
    XmlReactiveFragment(layoutId) {

    private var currentTriggerIndex = 0
    private val triggers = HookTriggers()

    fun distance(
        location: Coordinate,
        threshold: Distance,
        highAccuracy: Boolean = true
    ): Boolean {
        val key = "trigger-$currentTriggerIndex"
        currentTriggerIndex++
        return triggers.distance(key, location, threshold, highAccuracy)
    }

    fun frequency(threshold: Duration): Boolean {
        val key = "trigger-$currentTriggerIndex"
        currentTriggerIndex++
        return triggers.frequency(key, threshold)
    }

    override fun onUpdate() {
        currentTriggerIndex = 0
        update()
    }

    fun useNavController(): NavController {
        return useMemo(useRootView()) { findNavController() }
    }

    /**
     * Called whenever the fragment should update its content. Mostly due to changes in state.
     */
    abstract fun update()
}