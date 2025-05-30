package com.kylecorry.trail_sense.shared.extensions

import androidx.annotation.LayoutRes
import com.kylecorry.andromeda.fragments.XmlReactiveBottomSheetFragment
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import java.time.Duration

abstract class TrailSenseReactiveBottomSheetFragment(@LayoutRes private val layoutId: Int) :
    XmlReactiveBottomSheetFragment(layoutId) {

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

    /**
     * Called whenever the fragment should update its content. Mostly due to changes in state.
     */
    abstract fun update()

    override fun onPause() {
        super.onPause()
        cleanupEffects()
    }
}